package com.bank.core.service;

import com.bank.common.enums.AccountStatus;
import com.bank.common.enums.AccountType;
import com.bank.common.enums.Currency;
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
    private TransactionRepository transactionRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private TransactionEventProducer transactionEventProducer;

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
        assertThat(captor.getValue().getAccountNumber()).startsWith("40817").hasSize(20);
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
    @DisplayName("Deposit increases balance and creates completed transaction")
    void depositIncreasesBalanceAndCreatesTransaction() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity account = account(1L, userId, new BigDecimal("100.00"));
        AccountDto accountDto = accountDto(1L, userId, new BigDecimal("150.00"));
        when(bankAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(bankAccountRepository.save(account)).thenReturn(account);
        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountMapper.toDto(account)).thenReturn(accountDto);

        AccountDto result = accountService.deposit(1L, userId, new BigDecimal("50.00")).getData();

        assertThat(result).isEqualTo(accountDto);
        assertThat(account.getBalance()).isEqualByComparingTo("150.00");

        ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getFromAccountId()).isNull();
        assertThat(transactionCaptor.getValue().getToAccountId()).isEqualTo(1L);
        assertThat(transactionCaptor.getValue().getAmount()).isEqualByComparingTo("50.00");
        assertThat(transactionCaptor.getValue().getConvertedAmount()).isEqualByComparingTo("50.00");
        assertThat(transactionCaptor.getValue().getCurrency()).isEqualTo(Currency.USD);
        assertThat(transactionCaptor.getValue().getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(transactionCaptor.getValue().getStatus()).isEqualTo(TransactionStatus.COMPLETED);

        ArgumentCaptor<TransactionEvent> eventCaptor = ArgumentCaptor.forClass(TransactionEvent.class);
        verify(transactionEventProducer).send(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(eventCaptor.getValue().getType()).isEqualTo(TransactionType.DEPOSIT.name());
    }

    @Test
    @DisplayName("Deposit rejects zero or negative amount")
    void depositRejectsZeroOrNegativeAmount() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> accountService.deposit(1L, userId, BigDecimal.ZERO))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> accountService.deposit(1L, userId, new BigDecimal("-1.00")))
                .isInstanceOf(ConflictException.class);

        verify(bankAccountRepository, never()).findByIdForUpdate(any());
        verify(bankAccountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deposit throws NotFoundException when account is missing")
    void depositThrowsNotFoundWhenAccountMissing() {
        UUID userId = UUID.randomUUID();
        when(bankAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.deposit(1L, userId, new BigDecimal("10.00")))
                .isInstanceOf(NotFoundException.class);

        verify(bankAccountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deposit throws ConflictException when account belongs to another user")
    void depositThrowsConflictWhenAccountBelongsToAnotherUser() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity account = account(1L, UUID.randomUUID(), new BigDecimal("100.00"));
        when(bankAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.deposit(1L, userId, new BigDecimal("10.00")))
                .isInstanceOf(ConflictException.class);

        verify(bankAccountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deposit throws ConflictException when account is closed")
    void depositThrowsConflictWhenAccountClosed() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity account = account(1L, userId, new BigDecimal("100.00"), AccountStatus.CLOSED);
        when(bankAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.deposit(1L, userId, new BigDecimal("10.00")))
                .isInstanceOf(ConflictException.class);

        verify(bankAccountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Withdraw decreases balance and creates completed transaction")
    void withdrawDecreasesBalanceAndCreatesTransaction() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity account = account(1L, userId, new BigDecimal("100.00"));
        AccountDto accountDto = accountDto(1L, userId, new BigDecimal("60.00"));
        when(bankAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(bankAccountRepository.save(account)).thenReturn(account);
        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountMapper.toDto(account)).thenReturn(accountDto);

        AccountDto result = accountService.withdraw(1L, userId, new BigDecimal("40.00")).getData();

        assertThat(result).isEqualTo(accountDto);
        assertThat(account.getBalance()).isEqualByComparingTo("60.00");

        ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getFromAccountId()).isEqualTo(1L);
        assertThat(transactionCaptor.getValue().getToAccountId()).isNull();
        assertThat(transactionCaptor.getValue().getAmount()).isEqualByComparingTo("40.00");
        assertThat(transactionCaptor.getValue().getConvertedAmount()).isEqualByComparingTo("40.00");
        assertThat(transactionCaptor.getValue().getCurrency()).isEqualTo(Currency.USD);
        assertThat(transactionCaptor.getValue().getType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(transactionCaptor.getValue().getStatus()).isEqualTo(TransactionStatus.COMPLETED);

        ArgumentCaptor<TransactionEvent> eventCaptor = ArgumentCaptor.forClass(TransactionEvent.class);
        verify(transactionEventProducer).send(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(eventCaptor.getValue().getType()).isEqualTo(TransactionType.WITHDRAWAL.name());
    }

    @Test
    @DisplayName("Withdraw rejects zero or negative amount")
    void withdrawRejectsZeroOrNegativeAmount() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> accountService.withdraw(1L, userId, BigDecimal.ZERO))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> accountService.withdraw(1L, userId, new BigDecimal("-1.00")))
                .isInstanceOf(ConflictException.class);

        verify(bankAccountRepository, never()).findByIdForUpdate(any());
        verify(bankAccountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Withdraw throws NotFoundException when account is missing")
    void withdrawThrowsNotFoundWhenAccountMissing() {
        UUID userId = UUID.randomUUID();
        when(bankAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.withdraw(1L, userId, new BigDecimal("10.00")))
                .isInstanceOf(NotFoundException.class);

        verify(bankAccountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Withdraw throws ConflictException when account belongs to another user")
    void withdrawThrowsConflictWhenAccountBelongsToAnotherUser() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity account = account(1L, UUID.randomUUID(), new BigDecimal("100.00"));
        when(bankAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.withdraw(1L, userId, new BigDecimal("10.00")))
                .isInstanceOf(ConflictException.class);

        verify(bankAccountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Withdraw throws ConflictException when account is closed")
    void withdrawThrowsConflictWhenAccountClosed() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity account = account(1L, userId, new BigDecimal("100.00"), AccountStatus.CLOSED);
        when(bankAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.withdraw(1L, userId, new BigDecimal("10.00")))
                .isInstanceOf(ConflictException.class);

        verify(bankAccountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Withdraw throws InsufficientFundsException when amount exceeds balance")
    void withdrawThrowsInsufficientFundsWhenAmountExceedsBalance() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity account = account(1L, userId, new BigDecimal("30.00"));
        when(bankAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.withdraw(1L, userId, new BigDecimal("40.00")))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(account.getBalance()).isEqualByComparingTo("30.00");
        verify(bankAccountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
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
                .email("user@example.com")
                .passwordHash("{noop}password")
                .role("USER")
                .enabled(true)
                .build();
    }

    private BankAccountEntity account(Long id, UUID userId, BigDecimal balance) {
        return account(id, userId, balance, AccountStatus.ACTIVE);
    }

    private BankAccountEntity account(Long id, UUID userId, BigDecimal balance, AccountStatus status) {
        return BankAccountEntity.builder()
                .id(id)
                .accountNumber(accountNumber(id))
                .userId(userId)
                .currency(Currency.USD)
                .balance(balance)
                .type(AccountType.CHECKING)
                .status(status)
                .build();
    }

    private AccountDto accountDto(Long id, UUID userId) {
        return accountDto(id, userId, BigDecimal.ZERO);
    }

    private AccountDto accountDto(Long id, UUID userId, BigDecimal balance) {
        return new AccountDto(
                id,
                accountNumber(id),
                userId,
                Currency.USD,
                balance,
                AccountType.CHECKING,
                AccountStatus.ACTIVE,
                null,
                null
        );
    }

    private String accountNumber(Long id) {
        return "40817" + String.format("%015d", id);
    }
}
