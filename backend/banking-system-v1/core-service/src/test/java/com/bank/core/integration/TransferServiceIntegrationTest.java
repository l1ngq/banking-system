package com.bank.core.integration;

import com.bank.common.enums.AccountStatus;
import com.bank.common.enums.AccountType;
import com.bank.common.enums.Currency;
import com.bank.common.exception.ConflictException;
import com.bank.core.dto.TransferRequest;
import com.bank.core.entity.BankAccountEntity;
import com.bank.core.entity.TransactionEntity;
import com.bank.core.entity.UserEntity;
import com.bank.core.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferServiceIntegrationTest extends BasePostgresIntegrationTest {

    private static final AtomicLong ACCOUNT_NUMBER_SEQUENCE = new AtomicLong(1);

    @Autowired
    private TransferService transferService;

    @Test
    @DisplayName("Успешный перевод одной валюты меняет балансы в БД")
    void transferSameCurrencyChangesBalancesInDatabase() {
        UserEntity user = userRepository.saveAndFlush(user("transfer1@example.com"));
        BankAccountEntity fromAccount = bankAccountRepository.saveAndFlush(account(user.getId(), new BigDecimal("100.00")));
        BankAccountEntity toAccount = bankAccountRepository.saveAndFlush(account(UUID.randomUUID(), new BigDecimal("20.00")));

        transferService.transfer(new TransferRequest(fromAccount.getId(), toAccount.getAccountNumber(), new BigDecimal("30.00"), Currency.USD), user.getId());

        assertThat(bankAccountRepository.findById(fromAccount.getId())).get()
                .extracting(BankAccountEntity::getBalance)
                .isEqualTo(new BigDecimal("70.00"));
        assertThat(bankAccountRepository.findById(toAccount.getId())).get()
                .extracting(BankAccountEntity::getBalance)
                .isEqualTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("После перевода создается TransactionEntity")
    void transferCreatesTransactionEntity() {
        UserEntity user = userRepository.saveAndFlush(user("transfer2@example.com"));
        BankAccountEntity fromAccount = bankAccountRepository.saveAndFlush(account(user.getId(), new BigDecimal("100.00")));
        BankAccountEntity toAccount = bankAccountRepository.saveAndFlush(account(UUID.randomUUID(), BigDecimal.ZERO));

        transferService.transfer(new TransferRequest(fromAccount.getId(), toAccount.getAccountNumber(), new BigDecimal("25.00"), Currency.USD), user.getId());

        assertThat(transactionRepository.findAll())
                .hasSize(1)
                .first()
                .extracting(TransactionEntity::getAmount)
                .isEqualTo(new BigDecimal("25.00"));
    }

    @Test
    @DisplayName("getHistory возвращает историю владельцу счета")
    void getHistoryReturnsHistoryForAccountOwner() {
        UserEntity user = userRepository.saveAndFlush(user("transfer3@example.com"));
        BankAccountEntity fromAccount = bankAccountRepository.saveAndFlush(account(user.getId(), new BigDecimal("100.00")));
        BankAccountEntity toAccount = bankAccountRepository.saveAndFlush(account(UUID.randomUUID(), BigDecimal.ZERO));
        transferService.transfer(new TransferRequest(fromAccount.getId(), toAccount.getAccountNumber(), new BigDecimal("10.00"), Currency.USD), user.getId());

        var response = transferService.getHistory(fromAccount.getId(), user.getId());

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).hasSize(1);
    }

    @Test
    @DisplayName("getHistory выбрасывает ConflictException для чужого счета")
    void getHistoryThrowsConflictForForeignAccount() {
        UserEntity owner = userRepository.saveAndFlush(user("transfer4@example.com"));
        BankAccountEntity account = bankAccountRepository.saveAndFlush(account(owner.getId(), BigDecimal.ZERO));

        assertThatThrownBy(() -> transferService.getHistory(account.getId(), UUID.randomUUID()))
                .isInstanceOf(ConflictException.class);
    }

    private UserEntity user(String email) {
        return UserEntity.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("{noop}password")
                .role("USER")
                .enabled(true)
                .build();
    }

    private BankAccountEntity account(UUID userId, BigDecimal balance) {
        return BankAccountEntity.builder()
                .accountNumber(nextAccountNumber())
                .userId(userId)
                .currency(Currency.USD)
                .balance(balance)
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    private String nextAccountNumber() {
        return "40817" + String.format("%015d", ACCOUNT_NUMBER_SEQUENCE.getAndIncrement());
    }
}
