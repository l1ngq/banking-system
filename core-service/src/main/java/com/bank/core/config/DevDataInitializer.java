package com.bank.core.config;

import com.bank.core.entity.UserEntity;
import com.bank.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@Profile("dev-auth")
@RequiredArgsConstructor
public class DevDataInitializer implements ApplicationRunner {

    private static final UUID DEV_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String DEV_EXTERNAL_AUTH_ID = "00000000-0000-0000-0000-000000000001";
    private static final String DEV_USER_EMAIL = "dev-user@bank.local";

    private final UserRepository userRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByExternalAuthId(DEV_EXTERNAL_AUTH_ID).isPresent()) {
            log.info("Dev user already exists: id={}, email={}", DEV_USER_ID, DEV_USER_EMAIL);
            return;
        }

        if (userRepository.findByEmail(DEV_USER_EMAIL).isPresent()) {
            log.info("Dev user email already exists: email={}", DEV_USER_EMAIL);
            return;
        }

        UserEntity user = userRepository.findById(DEV_USER_ID)
                .orElseGet(() -> UserEntity.builder()
                        .id(DEV_USER_ID)
                        .build());
        user.setExternalAuthId(DEV_EXTERNAL_AUTH_ID);
        user.setEmail(DEV_USER_EMAIL);

        userRepository.save(user);
        log.info("Created dev user: id={}, externalAuthId={}, email={}",
                user.getId(), DEV_EXTERNAL_AUTH_ID, DEV_USER_EMAIL);
    }
}
