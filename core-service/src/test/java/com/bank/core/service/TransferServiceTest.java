package com.bank.core.service;

import com.bank.common.enums.AccountStatus;
import com.bank.common.enums.AccountType;
import com.bank.common.enums.Currency;
import com.bank.core.dto.TransferRequest;
import com.bank.core.entity.BankAccountEntity;
import com.bank.core.kafka.producer.InterestEventProducer;
import com.bank.core.kafka.producer.TransactionEventProducer;
import com.bank.core.repository.BankAccountRepository;
import com.bank.core.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class TransferServiceTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

    @Autowired
    private TransferService transferService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

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
        registry.add("CURRENCIES_SERVICE_URL", () -> "http://localhost:8081");
        registry.add("BOOTSTRAP_SERVERS", () -> "localhost:9092");
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @AfterEach
    void tearDown() {
        transactionRepository.deleteAll();
        bankAccountRepository.deleteAll();
    }

    @Test
    void transferChangesBalancesForAccountsWithSameCurrency() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity fromAccount = createAccount(userId, new BigDecimal("100.00"));
        BankAccountEntity toAccount = createAccount(UUID.randomUUID(), new BigDecimal("20.00"));

        transferService.transfer(new TransferRequest(
                fromAccount.getId(),
                toAccount.getId(),
                new BigDecimal("30.00"),
                Currency.RUB), userId);

        BankAccountEntity updatedFromAccount = bankAccountRepository.findById(fromAccount.getId()).orElseThrow();
        BankAccountEntity updatedToAccount = bankAccountRepository.findById(toAccount.getId()).orElseThrow();

        assertThat(updatedFromAccount.getBalance()).isEqualByComparingTo("70.00");
        assertThat(updatedToAccount.getBalance()).isEqualByComparingTo("50.00");
        assertThat(transactionRepository.findAll()).hasSize(1);
    }

    @Test
    void concurrentOppositeTransfersDoNotDeadlock() {
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(10), () -> {
            UUID userId = UUID.randomUUID();
            BankAccountEntity firstAccount = createAccount(userId, new BigDecimal("100.00"));
            BankAccountEntity secondAccount = createAccount(userId, new BigDecimal("100.00"));

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService executorService = Executors.newFixedThreadPool(2);

            try {
                Callable<Void> firstTransfer = () -> {
                    ready.countDown();
                    start.await();
                    transferService.transfer(new TransferRequest(
                            firstAccount.getId(),
                            secondAccount.getId(),
                            new BigDecimal("10.00"),
                            Currency.RUB), userId);
                    return null;
                };

                Callable<Void> secondTransfer = () -> {
                    ready.countDown();
                    start.await();
                    transferService.transfer(new TransferRequest(
                            secondAccount.getId(),
                            firstAccount.getId(),
                            new BigDecimal("20.00"),
                            Currency.RUB), userId);
                    return null;
                };

                List<Future<Void>> futures = List.of(
                        executorService.submit(firstTransfer),
                        executorService.submit(secondTransfer));

                assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
                start.countDown();

                for (Future<Void> future : futures) {
                    future.get(5, TimeUnit.SECONDS);
                }
            } finally {
                executorService.shutdownNow();
            }

            BankAccountEntity updatedFirstAccount = bankAccountRepository.findById(firstAccount.getId()).orElseThrow();
            BankAccountEntity updatedSecondAccount = bankAccountRepository.findById(secondAccount.getId()).orElseThrow();

            assertThat(updatedFirstAccount.getBalance()).isEqualByComparingTo("110.00");
            assertThat(updatedSecondAccount.getBalance()).isEqualByComparingTo("90.00");
            assertThat(transactionRepository.findAll()).hasSize(2);
        });
    }

    private BankAccountEntity createAccount(UUID userId, BigDecimal balance) {
        return bankAccountRepository.save(BankAccountEntity.builder()
                .userId(userId)
                .currency(Currency.RUB)
                .balance(balance)
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build());
    }
}
