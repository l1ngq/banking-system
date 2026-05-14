package com.bank.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthConverterTest {

    private final JwtAuthConverter converter = new JwtAuthConverter();

    @Test
    @DisplayName("Casdoor roles[].name USER maps to ROLE_USER")
    void casdoorUserRoleMapsToRoleUser() {
        JwtAuthenticationToken authentication = convert(Map.of(
                "roles", List.of(Map.of("name", "USER"))
        ));

        assertThat(authorities(authentication)).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("Casdoor roles[].name ADMIN maps to ROLE_ADMIN")
    void casdoorAdminRoleMapsToRoleAdmin() {
        JwtAuthenticationToken authentication = convert(Map.of(
                "roles", List.of(Map.of("name", "ADMIN"))
        ));

        assertThat(authorities(authentication)).containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Legacy realm_access.roles maps to ROLE authorities")
    void realmAccessRolesMapToAuthorities() {
        JwtAuthenticationToken authentication = convert(Map.of(
                "realm_access", Map.of("roles", List.of("user", "ROLE_admin"))
        ));

        assertThat(authorities(authentication)).containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Empty roles fallback to ROLE_USER")
    void emptyRolesFallbackToRoleUser() {
        JwtAuthenticationToken authentication = convert(Map.of(
                "roles", List.of()
        ));

        assertThat(authorities(authentication)).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("OpenID profile email scopes are not converted to roles")
    void oidcScopesAreNotConvertedToRoles() {
        JwtAuthenticationToken authentication = convert(Map.of(
                "scope", "openid profile email"
        ));

        assertThat(authorities(authentication))
                .containsExactly("ROLE_USER")
                .doesNotContain("ROLE_OPENID", "ROLE_PROFILE", "ROLE_EMAIL");
    }

    private JwtAuthenticationToken convert(Map<String, Object> claims) {
        return (JwtAuthenticationToken) converter.convert(jwt(claims));
    }

    private Jwt jwt(Map<String, Object> claims) {
        Instant now = Instant.now();
        return new Jwt(
                "token",
                now,
                now.plusSeconds(60),
                Map.of("alg", "none"),
                claims
        );
    }

    private List<String> authorities(JwtAuthenticationToken authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
