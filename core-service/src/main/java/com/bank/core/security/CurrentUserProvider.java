package com.bank.core.security;

import com.bank.common.exception.NotFoundException;
import com.bank.core.entity.UserEntity;
import com.bank.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;

    @Transactional
    public CurrentUserInfo getCurrentUser() {
        Authentication authentication = currentAuthentication();
        String email = authentication.getName();
        if (email == null || email.isBlank()) {
            throw new NotFoundException("Authenticated session user does not contain email");
        }

        UserEntity user = userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("User not found by email: " + email));
        return new CurrentUserInfo(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
    }

    private Authentication currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            throw new NotFoundException("Authenticated session user was not found in SecurityContext");
        }
        return authentication;
    }
}
