package com.bank.core.dto.auth;

import java.time.Instant;
import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String email,
        String role,
        Instant createdAt
) {
}
