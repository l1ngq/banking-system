package com.bank.core.controller;

import com.bank.common.dto.UniversalResponse;
import com.bank.core.dto.auth.AuthStateResponse;
import com.bank.core.dto.auth.RegisterRequest;
import com.bank.core.dto.auth.RegisterResponse;
import com.bank.core.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private static final String ROLE_PREFIX = "ROLE_";

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UniversalResponse<AuthStateResponse>> me(
            Authentication authentication,
            CsrfToken csrfToken) {
        csrfToken.getToken();

        if (!isAuthenticated(authentication)) {
            AuthStateResponse response = new AuthStateResponse(false, null, null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UniversalResponse<>(response));
        }

        AuthStateResponse response = new AuthStateResponse(
                true,
                authentication.getName(),
                currentRole(authentication)
        );
        return ResponseEntity.ok(new UniversalResponse<>(response));
    }

    @PostMapping("/registration")
    public UniversalResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Request to register user with email={}", request.getEmail());
        return userService.register(request);
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.isAuthenticated();
    }

    private String currentRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .findFirst()
                .orElse(null);
    }
}
