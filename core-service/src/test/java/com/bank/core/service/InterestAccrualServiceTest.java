package com.bank.core.service;

import com.bank.common.enums.AccountStatus;
import com.bank.common.enums.AccountType;
import com.bank.common.enums.Currency;
import com.bank.core.entity.BankAccountEntity;
import com.bank.core.kafka.producer.InterestEventProducer;
import com.bank.core.kafka.producer.TransactionEventProducer;
import com.bank.core.repository.BankAccountRepository;
import com.bank.core.repository.InterestAccrualLogRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class InterestAccrualServiceTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

    @Autowired
    private InterestAccrualService interestAccrualService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private InterestAccrualLogRepository interestAccrualLogRepository;

    @MockitoBean
    private InterestEventProducer interestEventProducer;

    @MockitoBean
    private TransactionEventProducer transactionEventProducer;

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
        interestAccrualLogRepository.deleteAll();
        bankAccountRepository.deleteAll();
    }

    @Test
    void accrueForAccountAddsDailyInterestAndWritesLog() {
        BankAccountEntity account = createSavingsAccount(new BigDecimal("365.00"));

        interestAccrualService.accrueForAccount(account);

        BankAccountEntity updatedAccount = bankAccountRepository.findById(account.getId()).orElseThrow();

        assertThat(updatedAccount.getBalance()).isEqualByComparingTo("365.05");
        assertThat(updatedAccount.getLastInterestAccruedDate()).isEqualTo(LocalDate.now(ZoneOffset.UTC));
        assertThat(interestAccrualLogRepository.findAll()).hasSize(1);
    }

    @Test
    void runAccrualForAllProcessesMoreThanOnePageWithoutSkippingAccounts() {
        IntStream.range(0, 101)
                .forEach(index -> createSavingsAccount(new BigDecimal("365.00")));

        interestAccrualService.runAccrualForAll();

        assertThat(bankAccountRepository.findAll())
                .allSatisfy(account -> {
                    assertThat(account.getBalance()).isEqualByComparingTo("365.05");
                    assertThat(account.getLastInterestAccruedDate()).isEqualTo(LocalDate.now(ZoneOffset.UTC));
                });
        assertThat(interestAccrualLogRepository.findAll()).hasSize(101);
    }

    private BankAccountEntity createSavingsAccount(BigDecimal balance) {
        return bankAccountRepository.save(BankAccountEntity.builder()
                .userId(UUID.randomUUID())
                .currency(Currency.RUB)
                .balance(balance)
                .type(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .build());
    }
}
