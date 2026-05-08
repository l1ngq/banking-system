package com.bank.core;

import com.bank.common.enums.AccountStatus;
import com.bank.common.enums.AccountType;
import com.bank.common.enums.Currency;
import com.bank.core.entity.BankAccountEntity;
import com.bank.core.entity.UserEntity;
import com.bank.core.kafka.producer.InterestEventProducer;
import com.bank.core.kafka.producer.TransactionEventProducer;
import com.bank.core.repository.BankAccountRepository;
import com.bank.core.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
public abstract class BaseTest {

    protected static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    protected static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    protected static final String USER_TOKEN = "user-token";
    protected static final String ADMIN_TOKEN = "admin-token";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18");

    @MockitoBean
    protected JwtDecoder jwtDecoder;

    @MockitoBean
    protected TransactionEventProducer transactionEventProducer;

    @MockitoBean
    protected InterestEventProducer interestEventProducer;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected BankAccountRepository bankAccountRepository;

    @Autowired
    protected UserRepository userRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("DATABASE_URL", POSTGRES::getJdbcUrl);
        registry.add("DATABASE_USERNAME", POSTGRES::getUsername);
        registry.add("DATABASE_PASSWORD", POSTGRES::getPassword);
        registry.add("APP_DATABASE_SCHEMA", () -> "public");
        registry.add("OIDC_ISSUER_URI", () -> "http://localhost:8082/realms/banking");
        registry.add("OIDC_CLIENT_ID", () -> "banking-core");
        registry.add("BOOTSTRAP_SERVERS", () -> "localhost:9092");
        registry.add("CURRENCIES_SERVICE_URL", () -> "http://localhost:8081");
        registry.add("CORS_ALLOWED_ORIGINS", () -> "http://localhost:3000");
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
        log.info("Test DB: {}", POSTGRES.getJdbcUrl());
    }

    @BeforeEach
    void tearDown() {
        bankAccountRepository.deleteAll();
        userRepository.deleteAll();
        lenient().when(jwtDecoder.decode(anyString())).thenAnswer(invocation -> {
            String token = invocation.getArgument(0);
            if (ADMIN_TOKEN.equals(token)) {
                return jwt(ADMIN_TOKEN, ADMIN_ID, "admin@bank.com", List.of("USER", "ADMIN"));
            }
            return jwt(USER_TOKEN, USER_ID, "user@bank.com", List.of("USER"));
        });
    }

    protected UserEntity createUser(UUID userId, String email) {
        return userRepository.save(UserEntity.builder()
                .id(userId)
                .keycloakId(userId.toString())
                .email(email)
                .build());
    }

    protected BankAccountEntity createAccount(UUID userId, Currency currency, BigDecimal balance) {
        return bankAccountRepository.save(BankAccountEntity.builder()
                .userId(userId)
                .currency(currency)
                .balance(balance)
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build());
    }

    private Jwt jwt(String token, UUID subject, String email, List<String> roles) {
        Instant now = Instant.now();
        return Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject(subject.toString())
                .claim("email", email)
                .claim("realm_access", Map.of("roles", roles))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .build();
    }
}
