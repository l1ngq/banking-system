package com.bank.core.controller;

import com.bank.common.dto.UniversalResponse;
import com.bank.core.dto.AccountDto;
import com.bank.core.mapper.AccountMapper;
import com.bank.core.repository.BankAccountRepository;
import com.bank.core.service.InterestAccrualService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final BankAccountRepository bankAccountRepository;
    private final AccountMapper accountMapper;
    private final InterestAccrualService interestAccrualService;

    @GetMapping("/accounts")
    public Page<AccountDto> getAllAccounts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        log.info("Request to get all accounts, page: {}, size: {}", page, size);
        return bankAccountRepository.findAll(PageRequest.of(page, size))
                .map(accountMapper::toDto);
    }

    @PostMapping("/interest/force-run")
    @PreAuthorize("hasRole('ADMIN')")
    public UniversalResponse<Void> forceRunInterestAccrual() {
        log.info("Force-run начисления процентов запрошен ADMIN-ом");
        interestAccrualService.runAccrualForAll();
        return new UniversalResponse<>(0, "Начисление процентов запущено");
    }
}
