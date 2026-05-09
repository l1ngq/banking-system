package com.bank.core.service;

import com.bank.common.enums.AccountStatus;
import com.bank.common.enums.AccountType;
import com.bank.common.enums.Currency;
import com.bank.common.exception.ConflictException;
import com.bank.common.exception.CurrencyServiceUnavailableException;
import com.bank.common.exception.InsufficientFundsException;
import com.bank.common.exception.NotFoundException;
import com.bank.core.client.CurrenciesClient;
import com.bank.core.dto.ConversionResult;
import com.bank.core.dto.TransferRequest;
import com.bank.core.entity.BankAccountEntity;
import com.bank.core.entity.TransactionEntity;
import com.bank.core.kafka.producer.TransactionEventProducer;
import com.bank.core.mapper.TransactionMapper;
import com.bank.core.repository.BankAccountRepository;
import com.bank.core.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import java.math.BigDecimal;
import java.util.function.Function;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceUnitTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CurrenciesClient currenciesClient;

    @Mock
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private TransactionEventProducer transactionEventProducer;

    @InjectMocks
    private TransferService transferService;

    @Test
    @DisplayName("Перевод на тот же самый счёт запрещён")
    void transferToSameAccountThrowsConflict() {
        UUID userId = UUID.randomUUID();

        // when / then
        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(1L, 1L, new BigDecimal("100.00"), Currency.USD), userId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void transferSameCurrencyChangesBalancesAndSavesTransaction() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity fromAccount = account(1L, userId, new BigDecimal("1000.00"));
        BankAccountEntity toAccount = account(2L, UUID.randomUUID(), BigDecimal.ZERO);
        when(bankAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(bankAccountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));
        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        transferService.transfer(new TransferRequest(1L, 2L, new BigDecimal("100.00"), Currency.USD), userId);

        assertThat(fromAccount.getBalance()).isEqualByComparingTo("900.00");
        assertThat(toAccount.getBalance()).isEqualByComparingTo("100.00");

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("100.00");
        assertThat(captor.getValue().getConvertedAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Если счёт отправителя не найден, выбрасывается NotFoundException")
    void transferThrowsNotFoundWhenSenderAccountMissing() {
        UUID userId = UUID.randomUUID();
        when(bankAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(1L, 2L, new BigDecimal("100.00"), Currency.USD), userId))
                .isInstanceOf(NotFoundException.class);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Если счёт получателя не найден, выбрасывается NotFoundException")
    void transferThrowsNotFoundWhenRecipientAccountMissing() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity fromAccount = account(1L, userId, new BigDecimal("1000.00"));
        when(bankAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(bankAccountRepository.findByIdForUpdate(2L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(1L, 2L, new BigDecimal("100.00"), Currency.USD), userId))
                .isInstanceOf(NotFoundException.class);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Если счёт отправителя закрыт, выбрасывается ConflictException")
    void transferThrowsConflictWhenSenderAccountClosed() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity fromAccount = account(1L, userId, new BigDecimal("1000.00"), Currency.USD, AccountStatus.CLOSED);
        BankAccountEntity toAccount = account(2L, UUID.randomUUID(), BigDecimal.ZERO);
        when(bankAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(bankAccountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));

        // when / then
        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(1L, 2L, new BigDecimal("100.00"), Currency.USD), userId))
                .isInstanceOf(ConflictException.class);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Если счёт получателя закрыт, выбрасывается ConflictException")
    void transferThrowsConflictWhenRecipientAccountClosed() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity fromAccount = account(1L, userId, new BigDecimal("1000.00"));
        BankAccountEntity toAccount = account(2L, UUID.randomUUID(), BigDecimal.ZERO, Currency.USD, AccountStatus.CLOSED);
        when(bankAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(bankAccountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));

        // when / then
        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(1L, 2L, new BigDecimal("100.00"), Currency.USD), userId))
                .isInstanceOf(ConflictException.class);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferThrowsWhenInsufficientFunds() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity fromAccount = account(1L, userId, new BigDecimal("50.00"));
        BankAccountEntity toAccount = account(2L, UUID.randomUUID(), BigDecimal.ZERO);
        when(bankAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(bankAccountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));

        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(1L, 2L, new BigDecimal("100.00"), Currency.USD), userId))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    @DisplayName("При недостатке средств транзакция не сохраняется")
    void transferDoesNotSaveTransactionWhenInsufficientFunds() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity fromAccount = account(1L, userId, new BigDecimal("50.00"));
        BankAccountEntity toAccount = account(2L, UUID.randomUUID(), BigDecimal.ZERO);
        when(bankAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(bankAccountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));

        // when
        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(1L, 2L, new BigDecimal("100.00"), Currency.USD), userId))
                .isInstanceOf(InsufficientFundsException.class);

        // then
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferThrowsWhenAccountDoesNotBelongToCurrentUser() {
        UUID currentUserId = UUID.randomUUID();
        BankAccountEntity fromAccount = account(1L, UUID.randomUUID(), new BigDecimal("1000.00"));
        BankAccountEntity toAccount = account(2L, UUID.randomUUID(), BigDecimal.ZERO);
        when(bankAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(bankAccountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));

        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(1L, 2L, new BigDecimal("100.00"), Currency.USD), currentUserId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Если currencies-service недоступен при переводе между валютами, выбрасывается CurrencyServiceUnavailableException")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void transferThrowsWhenCurrenciesServiceUnavailable() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity fromAccount = account(1L, userId, new BigDecimal("1000.00"), Currency.USD, AccountStatus.ACTIVE);
        BankAccountEntity toAccount = account(2L, UUID.randomUUID(), BigDecimal.ZERO, Currency.RUB, AccountStatus.ACTIVE);
        when(bankAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(bankAccountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));
        when(circuitBreakerFactory.create("currencies")).thenReturn(circuitBreaker);
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(invocation -> {
                    Function<Throwable, BigDecimal> fallback = invocation.getArgument(1);
                    return fallback.apply(new RuntimeException("currencies-service down"));
                });

        // when / then
        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(1L, 2L, new BigDecimal("100.00"), Currency.USD), userId))
                .isInstanceOf(CurrencyServiceUnavailableException.class);
    }

    @Test
    @DisplayName("При ошибке перевода балансы не изменяются")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void transferDoesNotChangeBalancesWhenCurrencyConversionFails() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity fromAccount = account(1L, userId, new BigDecimal("1000.00"), Currency.USD, AccountStatus.ACTIVE);
        BankAccountEntity toAccount = account(2L, UUID.randomUUID(), new BigDecimal("500.00"), Currency.RUB, AccountStatus.ACTIVE);
        when(bankAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(bankAccountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));
        when(circuitBreakerFactory.create("currencies")).thenReturn(circuitBreaker);
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(invocation -> {
                    Function<Throwable, BigDecimal> fallback = invocation.getArgument(1);
                    return fallback.apply(new RuntimeException("currencies-service down"));
                });

        // when
        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(1L, 2L, new BigDecimal("100.00"), Currency.USD), userId))
                .isInstanceOf(CurrencyServiceUnavailableException.class);

        // then
        assertThat(fromAccount.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(toAccount.getBalance()).isEqualByComparingTo("500.00");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("При переводе между разными валютами используется сумма из currencies-service")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void transferDifferentCurrenciesUsesConvertedAmount() {
        UUID userId = UUID.randomUUID();
        BankAccountEntity fromAccount = account(1L, userId, new BigDecimal("1000.00"), Currency.USD, AccountStatus.ACTIVE);
        BankAccountEntity toAccount = account(2L, UUID.randomUUID(), new BigDecimal("500.00"), Currency.RUB, AccountStatus.ACTIVE);
        when(bankAccountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(bankAccountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));
        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(circuitBreakerFactory.create("currencies")).thenReturn(circuitBreaker);
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(invocation -> {
                    Supplier<BigDecimal> supplier = invocation.getArgument(0);
                    return supplier.get();
                });
        when(currenciesClient.convert(eq("USD"), eq("RUB"), eq(new BigDecimal("10.00"))))
                .thenReturn(new com.bank.common.dto.UniversalResponse<>(
                        new ConversionResult("USD", "RUB", new BigDecimal("10.00"), new BigDecimal("900.00"), new BigDecimal("90.00"), null)));

        // when
        transferService.transfer(new TransferRequest(1L, 2L, new BigDecimal("10.00"), Currency.USD), userId);

        // then
        assertThat(fromAccount.getBalance()).isEqualByComparingTo("990.00");
        assertThat(toAccount.getBalance()).isEqualByComparingTo("1400.00");
    }

    private BankAccountEntity account(Long id, UUID userId, BigDecimal balance) {
        return account(id, userId, balance, Currency.USD, AccountStatus.ACTIVE);
    }

    private BankAccountEntity account(Long id, UUID userId, BigDecimal balance, Currency currency, AccountStatus status) {
        return BankAccountEntity.builder()
                .id(id)
                .userId(userId)
                .currency(currency)
                .balance(balance)
                .type(AccountType.CHECKING)
                .status(status)
                .build();
    }
}
