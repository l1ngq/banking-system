package com.bank.core.service;

import com.bank.common.enums.AccountStatus;
import com.bank.common.enums.AccountType;
import com.bank.common.enums.Currency;
import com.bank.common.exception.CurrencyServiceUnavailableException;
import com.bank.core.dto.TransferRequest;
import com.bank.core.entity.BankAccountEntity;
import com.bank.core.kafka.producer.InterestEventProducer;
import com.bank.core.kafka.producer.TransactionEventProducer;
import com.bank.core.repository.BankAccountRepository;
import com.bank.core.repository.TransactionRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class TransferCurrencyConversionTest {

    private static final StubCurrenciesServer CURRENCIES_SERVER = StubCurrenciesServer.start();

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

    @Autowired
    private TransferService transferService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockitoBean
    private TransactionEventProducer transactionEventProducer;

    @MockitoBean
    private InterestEventProducer interestEventProducer;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("DATABASE_URL", POSTGRES::getJdbcUrl);
        registry.add("DATABASE_USERNAME", POSTGRES::getUsername);
        registry.add("DATABASE_PASSWORD", POSTGRES::getPassword);
        registry.add("APP_DATABASE_SCHEMA", () -> "public");
        registry.add("OIDC_ISSUER_URI", () -> "http://localhost:8082/realms/banking");
        registry.add("OIDC_CLIENT_ID", () -> "banking-core");
        registry.add("CORS_ALLOWED_ORIGINS", () -> "http://localhost:3000");
        registry.add("CURRENCIES_SERVICE_URL", CURRENCIES_SERVER::url);
        registry.add("BOOTSTRAP_SERVERS", () -> "localhost:9092");
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @AfterEach
    void tearDown() {
        transactionRepository.deleteAll();
        bankAccountRepository.deleteAll();
        CURRENCIES_SERVER.startIfStopped();
        circuitBreakerRegistry.circuitBreaker("currencies").reset();
    }

    @AfterAll
    static void stopServer() {
        CURRENCIES_SERVER.stop();
    }

    @Test
    void usdToRubTransferUsesCurrenciesConversion() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity fromAccount = createAccount(userId, Currency.USD, new BigDecimal("100.00"));
        BankAccountEntity toAccount = createAccount(UUID.randomUUID(), Currency.RUB, new BigDecimal("1000.00"));

        transferService.transfer(new com.bank.core.dto.TransferRequest(
                fromAccount.getId(),
                toAccount.getId(),
                new BigDecimal("10.00"),
                Currency.USD), userId);

        BankAccountEntity updatedFromAccount = bankAccountRepository.findById(fromAccount.getId()).orElseThrow();
        BankAccountEntity updatedToAccount = bankAccountRepository.findById(toAccount.getId()).orElseThrow();

        assertThat(updatedFromAccount.getBalance()).isEqualByComparingTo("90.00");
        assertThat(updatedToAccount.getBalance()).isEqualByComparingTo("1900.00");
        assertThat(transactionRepository.findAll()).singleElement()
                .satisfies(transaction -> {
                    assertThat(transaction.getAmount()).isEqualByComparingTo("10.00");
                    assertThat(transaction.getConvertedAmount()).isEqualByComparingTo("900.00");
                });
    }

    @Test
    void currenciesCircuitBreakerOpensAfterFailuresAndReturns503WithoutTimeout() {
        UUID userId = UUID.randomUUID();
        CURRENCIES_SERVER.stop();

        for (int i = 0; i < 5; i++) {
            BankAccountEntity fromAccount = createAccount(userId, Currency.USD, new BigDecimal("100.00"));
            BankAccountEntity toAccount = createAccount(UUID.randomUUID(), Currency.RUB, new BigDecimal("1000.00"));

            assertThatThrownBy(() -> transferService.transfer(new TransferRequest(
                    fromAccount.getId(),
                    toAccount.getId(),
                    new BigDecimal("10.00"),
                    Currency.USD), userId))
                    .isInstanceOf(CurrencyServiceUnavailableException.class)
                    .extracting("httpCode")
                    .isEqualTo(503);
        }

        assertThat(circuitBreakerRegistry.circuitBreaker("currencies").getState())
                .isEqualTo(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN);

        BankAccountEntity fromAccount = createAccount(userId, Currency.USD, new BigDecimal("100.00"));
        BankAccountEntity toAccount = createAccount(UUID.randomUUID(), Currency.RUB, new BigDecimal("1000.00"));

        assertTimeoutPreemptively(Duration.ofMillis(500), () ->
                assertThatThrownBy(() -> transferService.transfer(new TransferRequest(
                        fromAccount.getId(),
                        toAccount.getId(),
                        new BigDecimal("10.00"),
                        Currency.USD), userId))
                        .isInstanceOf(CurrencyServiceUnavailableException.class)
                        .extracting("httpCode")
                        .isEqualTo(503));
    }

    private BankAccountEntity createAccount(UUID userId, Currency currency, BigDecimal balance) {
        return bankAccountRepository.save(BankAccountEntity.builder()
                .userId(userId)
                .currency(currency)
                .balance(balance)
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build());
    }

    private static final class StubCurrenciesServer {

        private HttpServer server;
        private ExecutorService executorService;
        private int port;

        static StubCurrenciesServer start() {
            StubCurrenciesServer stub = new StubCurrenciesServer();
            stub.startIfStopped();
            return stub;
        }

        synchronized void startIfStopped() {
            if (server != null) {
                return;
            }
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                port = server.getAddress().getPort();
                server.createContext("/api/currencies/convert", this::handleConvert);
                executorService = Executors.newSingleThreadExecutor();
                server.setExecutor(executorService);
                server.start();
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to start currencies stub", ex);
            }
        }

        synchronized void stop() {
            if (server != null) {
                server.stop(0);
                server = null;
            }
            if (executorService != null) {
                executorService.shutdownNow();
                executorService = null;
            }
        }

        String url() {
            return "http://localhost:" + port;
        }

        private void handleConvert(HttpExchange exchange) throws IOException {
            byte[] body = """
                    {"code":0,"message":"SUCCESS","data":{"from":"USD","to":"RUB","amount":10.00,"convertedAmount":900.00,"rate":90.00,"timestamp":"2026-05-07T00:00:00Z"}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(body);
            }
        }
    }
}
