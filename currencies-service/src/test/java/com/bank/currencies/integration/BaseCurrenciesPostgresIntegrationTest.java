package com.bank.currencies.integration;

import com.bank.currencies.client.FrankfurterClient;
import com.bank.currencies.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class BaseCurrenciesPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("currencies_test_db")
            .withUsername("bank")
            .withPassword("bank");

    @Autowired
    protected ExchangeRateRepository exchangeRateRepository;

    @MockitoBean
    protected FrankfurterClient frankfurterClient;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("DATABASE_URL", postgres::getJdbcUrl);
        registry.add("CURRENCIES_DATABASE_URL", postgres::getJdbcUrl);
        registry.add("DATABASE_USERNAME", postgres::getUsername);
        registry.add("DATABASE_PASSWORD", postgres::getPassword);
        registry.add("APP_DATABASE_SCHEMA", () -> "public");
        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("spring.liquibase.default-schema", () -> "public");
        registry.add("spring.liquibase.parameters.APP_DATABASE_SCHEMA", () -> "public");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "public");
        registry.add("spring.task.scheduling.enabled", () -> "false");
    }

    @BeforeEach
    void cleanDatabase() {
        exchangeRateRepository.deleteAll();
    }
}
