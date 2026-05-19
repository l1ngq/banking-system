package com.bank.core.service;

import com.bank.common.dto.UniversalResponse;
import com.bank.common.enums.AccountStatus;
import com.bank.common.enums.TransactionStatus;
import com.bank.common.enums.TransactionType;
import com.bank.common.event.TransactionEvent;
import com.bank.common.exception.ConflictException;
import com.bank.common.exception.InsufficientFundsException;
import com.bank.common.exception.NotFoundException;
import com.bank.core.dto.AccountDto;
import com.bank.core.dto.AccountListDto;
import com.bank.core.dto.CreateAccountRequest;
import com.bank.core.entity.BankAccountEntity;
import com.bank.core.entity.TransactionEntity;
import com.bank.core.entity.UserEntity;
import com.bank.core.kafka.producer.TransactionEventProducer;
import com.bank.core.mapper.AccountMapper;
import com.bank.core.repository.BankAccountRepository;
import com.bank.core.repository.TransactionRepository;
import com.bank.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
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
    private final TransactionRepository transactionRepository;
    private final AccountMapper accountMapper;
    private final TransactionEventProducer transactionEventProducer;

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
    public UniversalResponse<AccountDto> deposit(Long accountId, UUID currentUserId, BigDecimal amount) {
        log.info("Request to deposit into accountId: {} for userId: {}", accountId, currentUserId);
        validatePositiveAmount(amount);

        BankAccountEntity account = getOwnedActiveAccountForUpdate(accountId, currentUserId);
        account.setBalance(account.getBalance().add(amount));
        BankAccountEntity savedAccount = bankAccountRepository.save(account);

        TransactionEntity transaction = saveAccountOperationTransaction(
                null,
                savedAccount.getId(),
                amount,
                savedAccount,
                TransactionType.DEPOSIT);
        sendTransactionEvent(transaction, currentUserId, amount, TransactionType.DEPOSIT);

        return new UniversalResponse<>(accountMapper.toDto(savedAccount));
    }

    @Transactional
    public UniversalResponse<AccountDto> withdraw(Long accountId, UUID currentUserId, BigDecimal amount) {
        log.info("Request to withdraw from accountId: {} for userId: {}", accountId, currentUserId);
        validatePositiveAmount(amount);

        BankAccountEntity account = getOwnedActiveAccountForUpdate(accountId, currentUserId);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        account.setBalance(account.getBalance().subtract(amount));
        BankAccountEntity savedAccount = bankAccountRepository.save(account);

        TransactionEntity transaction = saveAccountOperationTransaction(
                savedAccount.getId(),
                null,
                amount,
                savedAccount,
                TransactionType.WITHDRAWAL);
        sendTransactionEvent(transaction, currentUserId, amount, TransactionType.WITHDRAWAL);

        return new UniversalResponse<>(accountMapper.toDto(savedAccount));
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

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ConflictException("Amount must be greater than zero");
        }
    }

    private BankAccountEntity getOwnedActiveAccountForUpdate(Long accountId, UUID currentUserId) {
        BankAccountEntity account = bankAccountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));

        if (!account.getUserId().equals(currentUserId)) {
            throw new ConflictException("Account does not belong to current user");
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ConflictException("Account is closed");
        }

        return account;
    }

    private TransactionEntity saveAccountOperationTransaction(
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount,
            BankAccountEntity account,
            TransactionType type) {
        TransactionEntity transaction = TransactionEntity.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(amount)
                .convertedAmount(amount)
                .currency(account.getCurrency())
                .type(type)
                .status(TransactionStatus.COMPLETED)
                .build();
        return transactionRepository.save(transaction);
    }

    private void sendTransactionEvent(
            TransactionEntity transaction,
            UUID currentUserId,
            BigDecimal amount,
            TransactionType type) {
        transactionEventProducer.send(TransactionEvent.builder()
                .transactionId(transaction.getId())
                .userId(currentUserId)
                .amount(amount)
                .currency(transaction.getCurrency().name())
                .type(type.name())
                .timestamp(Instant.now())
                .build());
    }
}
