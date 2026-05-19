package com.bank.core.integration;

import com.bank.core.client.CurrenciesClient;
import com.bank.core.kafka.producer.InterestEventProducer;
import com.bank.core.kafka.producer.TransactionEventProducer;
import com.bank.core.repository.BankAccountRepository;
import com.bank.core.repository.InterestAccrualLogRepository;
import com.bank.core.repository.TransactionRepository;
import com.bank.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class BasePostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("core_test_db")
            .withUsername("bank")
            .withPassword("bank");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:8.6.0")
            .withExposedPorts(6379);

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected BankAccountRepository bankAccountRepository;

    @Autowired
    protected TransactionRepository transactionRepository;

    @Autowired
    protected InterestAccrualLogRepository interestAccrualLogRepository;

    @MockitoBean
    protected TransactionEventProducer transactionEventProducer;

    @MockitoBean
    protected InterestEventProducer interestEventProducer;

    @MockitoBean
    protected CurrenciesClient currenciesClient;

    @MockitoBean
    protected ProducerFactory<Object, Object> producerFactory;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("DATABASE_URL", postgres::getJdbcUrl);
        registry.add("CORE_DATABASE_URL", postgres::getJdbcUrl);
        registry.add("DATABASE_USERNAME", postgres::getUsername);
        registry.add("DATABASE_PASSWORD", postgres::getPassword);
        registry.add("APP_DATABASE_SCHEMA", () -> "public");
        registry.add("CORS_ALLOWED_ORIGINS", () -> "http://localhost:3000");
        registry.add("REDIS_HOST", redis::getHost);
        registry.add("REDIS_PORT", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("BOOTSTRAP_SERVERS", () -> "localhost:9092");
        registry.add("CURRENCIES_SERVICE_URL", () -> "http://localhost:8081");
        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("spring.liquibase.default-schema", () -> "public");
        registry.add("spring.liquibase.parameters.APP_DATABASE_SCHEMA", () -> "public");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "public");
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
        registry.add("spring.task.scheduling.enabled", () -> "false");
    }

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        interestAccrualLogRepository.deleteAll();
        bankAccountRepository.deleteAll();
        userRepository.deleteAll();
    }
}
