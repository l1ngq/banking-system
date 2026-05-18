package com.bank.core.integration;

import com.bank.common.enums.AccountStatus;
import com.bank.common.enums.AccountType;
import com.bank.common.enums.Currency;
import com.bank.common.exception.ConflictException;
import com.bank.core.dto.AccountDto;
import com.bank.core.dto.AccountListDto;
import com.bank.core.dto.CreateAccountRequest;
import com.bank.core.entity.BankAccountEntity;
import com.bank.core.entity.UserEntity;
import com.bank.core.service.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountServiceIntegrationTest extends BasePostgresIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Test
    @DisplayName("createAccount создает счет для существующего пользователя")
    void createAccountCreatesAccountForExistingUser() {
        UserEntity user = userRepository.saveAndFlush(user("account1@example.com"));
        CreateAccountRequest request = new CreateAccountRequest(Currency.USD, AccountType.CHECKING);

        AccountDto result = accountService.createAccount(request, user.getId()).getData();

        assertThat(result.getId()).isNotNull();
        assertThat(result.getUserId()).isEqualTo(user.getId());
        assertThat(bankAccountRepository.findAllByUserId(user.getId())).hasSize(1);
    }

    @Test
    @DisplayName("getMyAccounts возвращает созданные счета")
    void getMyAccountsReturnsCreatedAccounts() {
        UserEntity user = userRepository.saveAndFlush(user("account2@example.com"));
        accountService.createAccount(new CreateAccountRequest(Currency.USD, AccountType.CHECKING), user.getId());
        accountService.createAccount(new CreateAccountRequest(Currency.EUR, AccountType.SAVINGS), user.getId());

        AccountListDto result = accountService.getMyAccounts(user.getId()).getData();

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getAccounts()).hasSize(2);
    }

    @Test
    @DisplayName("closeAccount закрывает свой счет с нулевым балансом")
    void closeAccountClosesOwnAccountWithZeroBalance() {
        UserEntity user = userRepository.saveAndFlush(user("account3@example.com"));
        BankAccountEntity account = bankAccountRepository.saveAndFlush(account(user.getId(), BigDecimal.ZERO));

        accountService.closeAccount(account.getId(), user.getId());

        assertThat(bankAccountRepository.findById(account.getId()))
                .get()
                .extracting(BankAccountEntity::getStatus)
                .isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    @DisplayName("closeAccount выбрасывает ConflictException для счета с ненулевым балансом")
    void closeAccountThrowsConflictForNonZeroBalance() {
        UserEntity user = userRepository.saveAndFlush(user("account4@example.com"));
        BankAccountEntity account = bankAccountRepository.saveAndFlush(account(user.getId(), new BigDecimal("10.00")));

        assertThatThrownBy(() -> accountService.closeAccount(account.getId(), user.getId()))
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
                .userId(userId)
                .currency(Currency.USD)
                .balance(balance)
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();
    }
}
