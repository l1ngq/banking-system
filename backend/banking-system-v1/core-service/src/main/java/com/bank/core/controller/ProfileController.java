package com.bank.core.controller;

import com.bank.common.dto.UniversalResponse;
import com.bank.core.dto.profile.ProfileDto;
import com.bank.core.dto.profile.UpdateProfileRequest;
import com.bank.core.security.CurrentUserProvider;
import com.bank.core.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
public class ProfileController {

    private final CurrentUserProvider currentUserProvider;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public UniversalResponse<ProfileDto> getProfile() {
        return userService.getProfile(currentUserProvider.getCurrentUser().localUserId());
    }

    @PatchMapping
    @PreAuthorize("isAuthenticated()")
    public UniversalResponse<ProfileDto> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(currentUserProvider.getCurrentUser().localUserId(), request);
    }
}
