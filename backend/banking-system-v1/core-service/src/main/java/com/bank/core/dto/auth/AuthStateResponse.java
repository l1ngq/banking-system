package com.bank.core.dto.auth;

public record AuthStateResponse(
        boolean authenticated,
        String email,
        String role
) {
}
