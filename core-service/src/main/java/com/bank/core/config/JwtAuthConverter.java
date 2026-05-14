package com.bank.core.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String GROUPS_CLAIM = "groups";
    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_NAME_FIELD = "name";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String SCOPE_CLAIM = "scope";
    private static final Set<String> APP_ROLE_SCOPES = Set.of();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractRoles(jwt).stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
                .collect(Collectors.toList());
        return new JwtAuthenticationToken(jwt, authorities);
    }

    private List<String> extractRoles(Jwt jwt) {
        List<String> roles = normalizeRoles(extractCasdoorRoles(jwt));
        if (!roles.isEmpty()) {
            return roles;
        }

        roles = normalizeRoles(extractRealmRoles(jwt));
        if (!roles.isEmpty()) {
            return roles;
        }

        roles = normalizeRoles(extractStringOrListClaim(jwt, GROUPS_CLAIM));
        if (!roles.isEmpty()) {
            return roles;
        }

        roles = normalizeRoles(extractAppScopes(jwt));
        if (!roles.isEmpty()) {
            return roles;
        }

        return List.of("USER");
    }

    private List<String> extractCasdoorRoles(Jwt jwt) {
        Object roles = jwt.getClaim(ROLES_CLAIM);
        if (roles instanceof List<?> list) {
            return list.stream()
                    .map(this::extractRoleName)
                    .filter(Objects::nonNull)
                    .toList();
        }
        return Collections.emptyList();
    }

    private String extractRoleName(Object role) {
        if (role instanceof Map<?, ?> roleMap) {
            Object name = roleMap.get(ROLE_NAME_FIELD);
            return name == null ? null : name.toString();
        }
        return role instanceof String roleName ? roleName : null;
    }

    private List<String> extractRealmRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
            return Collections.emptyList();
        }
        Object roles = realmAccessMap.get(ROLES_CLAIM);
        if (!(roles instanceof List<?> list)) {
            return Collections.emptyList();
        }
        return list.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    private List<String> extractStringOrListClaim(Jwt jwt, String claimName) {
        Object claim = jwt.getClaim(claimName);
        if (claim instanceof String value) {
            return splitClaim(value);
        }
        if (claim instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return Collections.emptyList();
    }

    private List<String> extractAppScopes(Jwt jwt) {
        return splitClaim(jwt.getClaimAsString(SCOPE_CLAIM)).stream()
                .map(scope -> scope.trim().toUpperCase())
                .filter(APP_ROLE_SCOPES::contains)
                .toList();
    }

    private List<String> splitClaim(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        return List.of(value.split("[,\\s]+"));
    }

    private List<String> normalizeRoles(List<String> roles) {
        LinkedHashSet<String> normalized = roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> role.toUpperCase())
                .map(role -> role.startsWith(ROLE_PREFIX) ? role.substring(ROLE_PREFIX.length()) : role)
                .filter(role -> !role.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return List.copyOf(normalized);
    }
}
