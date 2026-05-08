package com.bank.core.controller;

import com.bank.common.enums.AccountStatus;
import com.bank.common.enums.AccountType;
import com.bank.common.enums.Currency;
import com.bank.core.entity.BankAccountEntity;
import com.bank.core.kafka.producer.InterestEventProducer;
import com.bank.core.kafka.producer.TransactionEventProducer;
import com.bank.core.repository.BankAccountRepository;
import com.bank.core.repository.InterestAccrualLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class AdminInterestAccrualControllerTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        interestAccrualLogRepository.deleteAll();
        bankAccountRepository.deleteAll();
    }

    @Test
    void forceRunInterestAccrualIncreasesSavingsBalanceAndIsIdempotent() throws Exception {
        BankAccountEntity account = bankAccountRepository.save(BankAccountEntity.builder()
                .userId(UUID.randomUUID())
                .currency(Currency.RUB)
                .balance(new BigDecimal("365.00"))
                .type(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .build());

        mockMvc.perform(post("/api/admin/interest/force-run")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("Начисление процентов запущено"));

        BankAccountEntity updatedAccount = bankAccountRepository.findById(account.getId()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo("365.05");
        assertThat(interestAccrualLogRepository.findAll()).hasSize(1);

        mockMvc.perform(post("/api/admin/interest/force-run")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("Начисление процентов запущено"));

        BankAccountEntity unchangedAccount = bankAccountRepository.findById(account.getId()).orElseThrow();
        assertThat(unchangedAccount.getBalance()).isEqualByComparingTo("365.05");
        assertThat(interestAccrualLogRepository.findAll()).hasSize(1);
    }
}
