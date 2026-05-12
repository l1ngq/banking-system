package com.bank.core.integration;

import com.bank.common.enums.AccountStatus;
import com.bank.common.enums.AccountType;
import com.bank.common.enums.Currency;
import com.bank.common.enums.TransactionStatus;
import com.bank.common.enums.TransactionType;
import com.bank.core.entity.BankAccountEntity;
import com.bank.core.entity.InterestAccrualLogEntity;
import com.bank.core.entity.TransactionEntity;
import com.bank.core.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CoreRepositoryIntegrationTest extends BasePostgresIntegrationTest {

    @Test
    @DisplayName("Spring context загружается с PostgreSQL и Liquibase")
    void contextLoads() {
        assertThat(userRepository).isNotNull();
    }

    @Test
    @DisplayName("UserRepository сохраняет и находит пользователя по id и externalAuthId")
    void userRepositorySavesAndFindsByIdAndExternalAuthId() {
        UserEntity user = user(UUID.randomUUID(), "external-1", "user1@example.com");

        userRepository.saveAndFlush(user);

        assertThat(userRepository.findById(user.getId()))
                .get()
                .extracting(UserEntity::getExternalAuthId)
                .isEqualTo("external-1");
        assertThat(userRepository.findByExternalAuthId("external-1"))
                .get()
                .extracting(UserEntity::getId)
                .isEqualTo(user.getId());
    }

    @Test
    @DisplayName("BankAccountRepository сохраняет счет и находит его по userId")
    void bankAccountRepositorySavesAndFindsByUserId() {
        UserEntity user = userRepository.saveAndFlush(user(UUID.randomUUID(), "external-2", "user2@example.com"));
        BankAccountEntity account = bankAccount(user.getId(), BigDecimal.ZERO);

        BankAccountEntity saved = bankAccountRepository.saveAndFlush(account);

        assertThat(bankAccountRepository.findAllByUserId(user.getId()))
                .extracting(BankAccountEntity::getId)
                .containsExactly(saved.getId());
    }

    @Test
    @DisplayName("TransactionRepository сохраняет транзакцию и находит ее по счету")
    void transactionRepositorySavesAndFindsByAccountId() {
        TransactionEntity transaction = transaction(1L, 2L, new BigDecimal("10.00"));

        TransactionEntity saved = transactionRepository.saveAndFlush(transaction);

        List<TransactionEntity> history = transactionRepository
                .findAllByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(1L, 1L);
        assertThat(history).extracting(TransactionEntity::getId).containsExactly(saved.getId());
    }

    @Test
    @DisplayName("InterestAccrualLogRepository сохраняет лог начисления")
    void interestAccrualLogRepositorySavesLog() {
        UUID userId = UUID.randomUUID();
        InterestAccrualLogEntity log = InterestAccrualLogEntity.builder()
                .accountId(1L)
                .userId(userId)
                .amount(new BigDecimal("1.23"))
                .accruedDate(LocalDate.now())
                .build();

        InterestAccrualLogEntity saved = interestAccrualLogRepository.saveAndFlush(log);

        assertThat(interestAccrualLogRepository.findById(saved.getId()))
                .get()
                .extracting(InterestAccrualLogEntity::getAmount)
                .isEqualTo(new BigDecimal("1.23"));
    }

    private UserEntity user(UUID id, String externalAuthId, String email) {
        return UserEntity.builder()
                .id(id)
                .externalAuthId(externalAuthId)
                .email(email)
                .build();
    }

    private BankAccountEntity bankAccount(UUID userId, BigDecimal balance) {
        return BankAccountEntity.builder()
                .userId(userId)
                .currency(Currency.USD)
                .balance(balance)
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    private TransactionEntity transaction(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        return TransactionEntity.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(amount)
                .convertedAmount(amount)
                .currency(Currency.USD)
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .build();
    }
}
