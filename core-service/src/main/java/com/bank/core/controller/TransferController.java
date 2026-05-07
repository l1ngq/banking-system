package com.bank.core.controller;

import com.bank.common.dto.UniversalResponse;
import com.bank.core.dto.TransactionDto;
import com.bank.core.dto.TransferRequest;
import com.bank.core.service.TransferService;
import com.bank.core.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public UniversalResponse<TransactionDto> transfer(@Valid @RequestBody TransferRequest request) {
        log.info("Request to transfer: {}", request);
        UUID userId = SecurityUtils.getCurrentUserId();
        return transferService.transfer(request, userId);
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public UniversalResponse<List<TransactionDto>> getHistory(@RequestParam Long accountId) {
        log.info("Request to get transfer history by accountId: {}", accountId);
        return transferService.getHistory(accountId);
    }
}
