package com.bank.core.security;

import java.util.UUID;

public record CurrentUserInfo(
        UUID localUserId,
        String externalAuthId,
        String email,
        String name,
        String organization
) {
}
