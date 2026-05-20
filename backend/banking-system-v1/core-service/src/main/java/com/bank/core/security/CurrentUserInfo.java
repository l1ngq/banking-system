package com.bank.core.security;

import java.util.UUID;

public record CurrentUserInfo(
        UUID localUserId,
        String email,
        String role
) {
}
