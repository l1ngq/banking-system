package com.bank.core.config;

import com.bank.core.config.properties.CorsProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Configuration
@Profile("dev-auth")
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class DevSecurityConfig {

    private static final String DEV_USER_ID = "00000000-0000-0000-0000-000000000001";
    private static final String DEV_USER_EMAIL = "dev-user@bank.local";

    @Bean
    public SecurityFilterChain devFilterChain(
            HttpSecurity http,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(devAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private OncePerRequestFilter devAuthenticationFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain) throws ServletException, IOException {
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    SecurityContextHolder.getContext().setAuthentication(devAuthentication());
                }
                filterChain.doFilter(request, response);
            }
        };
    }

    private AbstractAuthenticationToken devAuthentication() {
        Instant issuedAt = Instant.now();
        Jwt jwt = new Jwt(
                "dev-auth-token",
                issuedAt,
                issuedAt.plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of(
                        "sub", DEV_USER_ID,
                        "email", DEV_USER_EMAIL
                )
        );
        return new JwtAuthenticationToken(jwt, List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        ));
    }
}
