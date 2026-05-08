package com.bank.core.service;

import com.bank.common.enums.AccountStatus;
import com.bank.common.enums.AccountType;
import com.bank.common.enums.Currency;
import com.bank.common.exception.ConflictException;
import com.bank.common.exception.InsufficientFundsException;
import com.bank.core.client.CurrenciesClient;
import com.bank.core.dto.TransferRequest;
import com.bank.core.entity.BankAccountEntity;
import com.bank.core.entity.TransactionEntity;
import com.bank.core.kafka.producer.TransactionEventProducer;
import com.bank.core.mapper.TransactionMapper;
import com.bank.core.repository.BankAccountRepository;
import com.bank.core.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private TransactionMapper transactionMapper;

    @Mock
    private TransactionEventProducer transactionEventProducer;

    @InjectMocks
    private TransferService transferService;

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
}
