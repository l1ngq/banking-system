package com.bank.core.service;

import com.bank.common.dto.UniversalResponse;
import com.bank.common.exception.ConflictException;
import com.bank.core.dto.auth.RegisterRequest;
import com.bank.core.dto.auth.RegisterResponse;
import com.bank.core.entity.UserEntity;
import com.bank.core.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService session auth")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void registerCreatesUserWithEncodedPassword() {
        RegisterRequest request = new RegisterRequest(" Student@Example.COM ", "password123");
        when(userRepository.existsByEmail("student@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setCreatedAt(Instant.parse("2026-05-15T00:00:00Z"));
            return user;
        });

        UniversalResponse<RegisterResponse> response = userService.register(request);

        assertThat(response.getData().email()).isEqualTo("student@example.com");
        assertThat(response.getData().role()).isEqualTo("USER");

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("student@example.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded-password");
        assertThat(captor.getValue().getRole()).isEqualTo("USER");
        assertThat(captor.getValue().isEnabled()).isTrue();
    }

    @Test
    void registerDuplicateEmailThrowsConflict() {
        RegisterRequest request = new RegisterRequest("student@example.com", "password123");
        when(userRepository.existsByEmail("student@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(ConflictException.class);

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void loadUserByUsernameReturnsRoleUser() {
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("student@example.com")
                .passwordHash("encoded-password")
                .role("USER")
                .enabled(true)
                .build();
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));

        UserDetails details = userService.loadUserByUsername(" Student@Example.COM ");

        assertThat(details.getUsername()).isEqualTo("student@example.com");
        assertThat(details.getPassword()).isEqualTo("encoded-password");
        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
        assertThat(details.isEnabled()).isTrue();
    }
}
