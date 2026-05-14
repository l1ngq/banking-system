package com.bank.core.security;

import com.bank.common.exception.NotFoundException;
import com.bank.core.entity.UserEntity;
import com.bank.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;

    @Transactional
    public CurrentUserInfo getCurrentUser() {
        Jwt jwt = currentJwt();
        String externalAuthId = firstNonBlank(jwt.getClaimAsString("id"), jwt.getSubject());
        if (externalAuthId == null) {
            throw new NotFoundException("JWT does not contain id or sub claim");
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new NotFoundException("JWT does not contain email claim");
        }

        UserEntity user = userRepository.findByExternalAuthId(externalAuthId)
                .or(() -> userRepository.findByEmail(email))
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .externalAuthId(externalAuthId)
                        .email(email)
                        .build()));

        return new CurrentUserInfo(
                user.getId(),
                externalAuthId,
                email,
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("owner")
        );
    }

    private Jwt currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new NotFoundException("Authenticated JWT user was not found in SecurityContext");
        }
        return jwtAuth.getToken();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }
}
