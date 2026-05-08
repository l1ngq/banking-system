package com.bank.core.util;

import com.bank.common.exception.NotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new NotFoundException("Не смог найти аутентифицированного пользователя в SecurityContext");
        }
        Jwt jwt = jwtAuth.getToken();
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new NotFoundException("Не смог найти subject в JWT токене");
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new NotFoundException("Subject JWT токена не является валидным UUID: " + subject, ex);
        }
    }

    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new NotFoundException("Не смог найти аутентифицированного пользователя в SecurityContext");
        }
        String email = jwtAuth.getToken().getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new NotFoundException("Не смог найти email в JWT токене");
        }
        return email;
    }
}
