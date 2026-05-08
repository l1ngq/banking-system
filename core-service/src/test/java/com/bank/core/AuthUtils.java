package com.bank.core;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AuthUtils {

    private AuthUtils() {
    }

    public static RequestPostProcessor jwtUser() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> {
                    jwt.subject("user-uuid-1");
                    jwt.claim("email", "user@bank.com");
                    jwt.claim("realm_access", Map.of("roles", List.of("USER")));
                    jwt.issuedAt(Instant.now());
                    jwt.expiresAt(Instant.now().plusSeconds(60));
                });
    }

    public static RequestPostProcessor jwtAdmin() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> {
                    jwt.subject("admin-uuid-1");
                    jwt.claim("email", "admin@bank.com");
                    jwt.claim("realm_access", Map.of("roles", List.of("USER", "ADMIN")));
                    jwt.issuedAt(Instant.now());
                    jwt.expiresAt(Instant.now().plusSeconds(60));
                });
    }
}
