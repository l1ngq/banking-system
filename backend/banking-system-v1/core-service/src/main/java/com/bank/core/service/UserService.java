package com.bank.core.service;

import com.bank.common.dto.UniversalResponse;
import com.bank.common.exception.ConflictException;
import com.bank.core.dto.auth.RegisterRequest;
import com.bank.core.dto.auth.RegisterResponse;
import com.bank.core.dto.profile.ProfileDto;
import com.bank.core.dto.profile.UpdateProfileRequest;
import com.bank.core.entity.UserEntity;
import com.bank.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UniversalResponse<RegisterResponse> register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("User with this email already exists");
        }

        UserEntity user = UserEntity.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(DEFAULT_ROLE)
                .enabled(true)
                .createdAt(Instant.now())
                .build();
        user = userRepository.save(user);

        return new UniversalResponse<>(new RegisterResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        ));
    }

    @Transactional(readOnly = true)
    public UniversalResponse<ProfileDto> getProfile(UUID userId) {
        UserEntity user = getUser(userId);
        return new UniversalResponse<>(toProfileDto(user));
    }

    @Transactional
    public UniversalResponse<ProfileDto> updateProfile(UUID userId, UpdateProfileRequest request) {
        UserEntity user = getUser(userId);
        user.setFullName(normalizeText(request.getFullName()));
        user.setPhone(normalizeText(request.getPhone()));
        user.setCity(normalizeText(request.getCity()));
        return new UniversalResponse<>(toProfileDto(userRepository.save(user)));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = normalizeEmail(username);
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found by email: " + email));

        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(roleWithoutPrefix(user.getRole()))
                .disabled(!user.isEnabled())
                .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private UserEntity getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found by id: " + userId));
    }

    private ProfileDto toProfileDto(UserEntity user) {
        return new ProfileDto(user.getFullName(), user.getPhone(), user.getCity(), user.getEmail(), user.getRole());
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String roleWithoutPrefix(String role) {
        if (role == null || role.isBlank()) {
            return DEFAULT_ROLE;
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring("ROLE_".length()) : normalized;
    }
}
