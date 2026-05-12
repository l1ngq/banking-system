package com.bank.core.service;

import com.bank.common.dto.UniversalResponse;
import com.bank.common.enums.AccountStatus;
import com.bank.common.exception.ConflictException;
import com.bank.common.exception.NotFoundException;
import com.bank.core.dto.AccountDto;
import com.bank.core.dto.AccountListDto;
import com.bank.core.dto.CreateAccountRequest;
import com.bank.core.entity.BankAccountEntity;
import com.bank.core.entity.UserEntity;
import com.bank.core.mapper.AccountMapper;
import com.bank.core.repository.BankAccountRepository;
import com.bank.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Слой бизнес логики
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;

    public UniversalResponse<AccountListDto> getMyAccounts(UUID userId) {
        log.info("Request to get accounts for userId: {}", userId);
        List<BankAccountEntity> accounts = bankAccountRepository.findAllByUserId(userId);
        List<AccountDto> dtoList = accountMapper.toDtoList(accounts);
        return new UniversalResponse<>(new AccountListDto(dtoList, dtoList.size()));
    }

    @Transactional
    public UniversalResponse<AccountDto> createAccount(CreateAccountRequest request, UUID userId) {
        log.info("Request to create account for userId: {}, request: {}", userId, request);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found by id: " + userId));

        BankAccountEntity entity = BankAccountEntity.builder()
                .userId(user.getId())
                .currency(request.getCurrency())
                .type(request.getType())
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        entity = bankAccountRepository.save(entity);
        return new UniversalResponse<>(accountMapper.toDto(entity));
    }

    @Transactional
    public UniversalResponse<Void> closeAccount(Long accountId, UUID userId) {
        log.info("Request to close account by id: {} for userId: {}", accountId, userId);

        BankAccountEntity account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Не смог найти счёт по id: " + accountId));

        if (!account.getUserId().equals(userId)) {
            throw new ConflictException("Счёт не принадлежит текущему пользователю");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new ConflictException("Нельзя закрыть счёт с ненулевым балансом");
        }

        account.setStatus(AccountStatus.CLOSED);
        bankAccountRepository.save(account);

        return new UniversalResponse<>(0, "SUCCESS");
    }
}
