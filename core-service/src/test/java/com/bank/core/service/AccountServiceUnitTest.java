package com.bank.core.service;

import com.bank.common.enums.AccountStatus;
import com.bank.common.enums.AccountType;
import com.bank.common.enums.Currency;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-тесты сервиса счетов")
class AccountServiceUnitTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService accountService;

    @Test
    @DisplayName("Возвращает список счетов пользователя")
    void getMyAccountsReturnsUserAccounts() {
        UUID userId = UUID.randomUUID();
        List<BankAccountEntity> accounts = List.of(account(1L, userId, BigDecimal.ZERO));
        List<AccountDto> accountDtos = List.of(accountDto(1L, userId));
        when(bankAccountRepository.findAllByUserId(userId)).thenReturn(accounts);
        when(accountMapper.toDtoList(accounts)).thenReturn(accountDtos);

        AccountListDto result = accountService.getMyAccounts(userId).getData();

        assertThat(result.getAccounts()).isEqualTo(accountDtos);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("Создает счет для существующего пользователя")
    void createAccountCreatesAccountForExistingUser() {
        UUID userId = UUID.randomUUID();
        UserEntity user = user(userId);
        CreateAccountRequest request = new CreateAccountRequest(Currency.USD, AccountType.CHECKING);
        AccountDto accountDto = accountDto(10L, userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bankAccountRepository.save(any(BankAccountEntity.class)))
                .thenAnswer(invocation -> {
                    BankAccountEntity saved = invocation.getArgument(0);
                    saved.setId(10L);
                    return saved;
                });
        when(accountMapper.toDto(any(BankAccountEntity.class))).thenReturn(accountDto);

        AccountDto result = accountService.createAccount(request, userId).getData();

        assertThat(result).isEqualTo(accountDto);
        ArgumentCaptor<BankAccountEntity> captor = ArgumentCaptor.forClass(BankAccountEntity.class);
        verify(bankAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getCurrency()).isEqualTo(Currency.USD);
        assertThat(captor.getValue().getType()).isEqualTo(AccountType.CHECKING);
        assertThat(captor.getValue().getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(captor.getValue().getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Выбрасывает NotFoundException, если пользователь не найден при создании счета")
    void createAccountThrowsNotFoundWhenUserMissing() {
        UUID userId = UUID.randomUUID();
        CreateAccountRequest request = new CreateAccountRequest(Currency.USD, AccountType.CHECKING);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.createAccount(request, userId))
                .isInstanceOf(NotFoundException.class);
        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Успешно закрывает свой счет с нулевым балансом")
    void closeAccountClosesOwnAccountWithZeroBalance() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity account = account(1L, userId, BigDecimal.ZERO);
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        accountService.closeAccount(1L, userId);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
        verify(bankAccountRepository).save(account);
    }

    @Test
    @DisplayName("Выбрасывает NotFoundException, если счет не найден")
    void closeAccountThrowsNotFoundWhenAccountMissing() {
        UUID userId = UUID.randomUUID();
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.closeAccount(1L, userId))
                .isInstanceOf(NotFoundException.class);
        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Выбрасывает ConflictException, если счет принадлежит другому пользователю")
    void closeAccountThrowsConflictWhenAccountBelongsToAnotherUser() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity account = account(1L, UUID.randomUUID(), BigDecimal.ZERO);
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.closeAccount(1L, userId))
                .isInstanceOf(ConflictException.class);
        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Выбрасывает ConflictException, если баланс ненулевой")
    void closeAccountThrowsConflictWhenBalanceIsNotZero() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity account = account(1L, userId, new BigDecimal("10.00"));
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.closeAccount(1L, userId))
                .isInstanceOf(ConflictException.class);
        verify(bankAccountRepository, never()).save(any());
    }

    private UserEntity user(UUID userId) {
        return UserEntity.builder()
                .id(userId)
                .externalAuthId(userId.toString())
                .email("user@example.com")
                .build();
    }

    private BankAccountEntity account(Long id, UUID userId, BigDecimal balance) {
        return BankAccountEntity.builder()
                .id(id)
                .userId(userId)
                .currency(Currency.USD)
                .balance(balance)
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    private AccountDto accountDto(Long id, UUID userId) {
        return new AccountDto(
                id,
                userId,
                Currency.USD,
                BigDecimal.ZERO,
                AccountType.CHECKING,
                AccountStatus.ACTIVE,
                null,
                null
        );
    }
}
