package com.bank.core.controller;

import com.bank.common.dto.UniversalResponse;
import com.bank.core.dto.AccountDto;
import com.bank.core.dto.AccountListDto;
import com.bank.core.dto.CreateAccountRequest;
import com.bank.core.security.CurrentUserProvider;
import com.bank.core.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public UniversalResponse<AccountListDto> getMyAccounts() {
        log.info("Request to get my accounts");
        UUID userId = currentUserProvider.getCurrentUser().localUserId();
        return accountService.getMyAccounts(userId);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public UniversalResponse<AccountDto> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        log.info("Request to create account: {}", request);
        UUID userId = currentUserProvider.getCurrentUser().localUserId();
        return accountService.createAccount(request, userId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public UniversalResponse<Void> closeAccount(@PathVariable("id") Long id) {
        log.info("Request to close account by id: {}", id);
        UUID userId = currentUserProvider.getCurrentUser().localUserId();
        return accountService.closeAccount(id, userId);
    }
}
