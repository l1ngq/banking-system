package com.bank.core.service;

import com.bank.common.enums.AccountStatus;
import com.bank.common.enums.AccountType;
import com.bank.common.enums.Currency;
import com.bank.core.entity.BankAccountEntity;
import com.bank.core.entity.InterestAccrualLogEntity;
import com.bank.core.kafka.producer.InterestEventProducer;
import com.bank.core.repository.BankAccountRepository;
import com.bank.core.repository.InterestAccrualLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterestAccrualServiceUnitTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private InterestAccrualLogRepository interestAccrualLogRepository;

    @Mock
    private InterestEventProducer interestEventProducer;

    @InjectMocks
    private InterestAccrualService interestAccrualService;

    @Test
    @DisplayName("CHECKING счёт не получает проценты")
    void checkingAccountDoesNotAccrueInterest() {
        BankAccountEntity account = account(AccountType.CHECKING, AccountStatus.ACTIVE, new BigDecimal("10000.00"),
                LocalDate.now(ZoneOffset.UTC).minusDays(1));

        // when
        interestAccrualService.accrueForAccount(account);

        // then
        assertThat(account.getBalance()).isEqualByComparingTo("10000.00");
        verify(bankAccountRepository, never()).save(any());
        verify(interestAccrualLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Закрытый счёт не получает проценты")
    void closedAccountDoesNotAccrueInterest() {
        BankAccountEntity account = account(AccountType.SAVINGS, AccountStatus.CLOSED, new BigDecimal("10000.00"),
                LocalDate.now(ZoneOffset.UTC).minusDays(1));

        // when
        interestAccrualService.accrueForAccount(account);

        // then
        assertThat(account.getBalance()).isEqualByComparingTo("10000.00");
        verify(bankAccountRepository, never()).save(any());
        verify(interestAccrualLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Счёт с нулевым балансом не получает проценты")
    void zeroBalanceAccountDoesNotAccrueInterest() {
        BankAccountEntity account = savingsAccount(BigDecimal.ZERO, LocalDate.now(ZoneOffset.UTC).minusDays(1));

        // when
        interestAccrualService.accrueForAccount(account);

        // then
        assertThat(account.getBalance()).isEqualByComparingTo("0.00");
        verify(bankAccountRepository, never()).save(any());
        verify(interestAccrualLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Проценты округляются до 2 знаков по HALF_UP")
    void accrueForAccountRoundsInterestHalfUpToTwoDigits() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        BankAccountEntity account = savingsAccount(new BigDecimal("109.50"), today.minusDays(1));

        // when
        interestAccrualService.accrueForAccount(account);

        // then
        assertThat(account.getBalance()).isEqualByComparingTo("109.52");
    }

    @Test
    void accrueForAccountAddsDailyInterest() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        BankAccountEntity account = savingsAccount(new BigDecimal("10000.00"), today.minusDays(1));

        interestAccrualService.accrueForAccount(account);

        BigDecimal expectedInterest = new BigDecimal("10000.00")
                .multiply(new BigDecimal("0.05"))
                .divide(new BigDecimal("365"), 2, java.math.RoundingMode.HALF_UP);
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("10000.00").add(expectedInterest));
        assertThat(account.getLastInterestAccruedDate()).isEqualTo(today);
        verify(bankAccountRepository).save(account);
    }

    @Test
    @DisplayName("После успешного начисления создаётся запись в логе")
    void accrueForAccountCreatesInterestAccrualLog() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        BankAccountEntity account = savingsAccount(new BigDecimal("365.00"), today.minusDays(1));

        // when
        interestAccrualService.accrueForAccount(account);

        // then
        ArgumentCaptor<InterestAccrualLogEntity> captor = ArgumentCaptor.forClass(InterestAccrualLogEntity.class);
        verify(interestAccrualLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAccountId()).isEqualTo(account.getId());
        assertThat(captor.getValue().getUserId()).isEqualTo(account.getUserId());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("0.05");
        assertThat(captor.getValue().getAccruedDate()).isEqualTo(today);
    }

    @Test
    void accrueForAccountDoesNothingWhenAlreadyAccruedToday() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        BankAccountEntity account = savingsAccount(new BigDecimal("10000.00"), today);

        interestAccrualService.accrueForAccount(account);

        assertThat(account.getBalance()).isEqualByComparingTo("10000.00");
        verify(bankAccountRepository, never()).save(any());
        verify(interestAccrualLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Повторное начисление за тот же день не создаёт новую запись в логе")
    void repeatedAccrualForSameDayDoesNotCreateLog() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        BankAccountEntity account = savingsAccount(new BigDecimal("365.00"), today);

        // when
        interestAccrualService.accrueForAccount(account);

        // then
        verify(interestAccrualLogRepository, never()).save(any());
    }

    private BankAccountEntity savingsAccount(BigDecimal balance, LocalDate lastInterestAccruedDate) {
        return account(AccountType.SAVINGS, AccountStatus.ACTIVE, balance, lastInterestAccruedDate);
    }

    private BankAccountEntity account(AccountType type, AccountStatus status, BigDecimal balance, LocalDate lastInterestAccruedDate) {
        return BankAccountEntity.builder()
                .id(1L)
                .userId(UUID.randomUUID())
                .currency(Currency.USD)
                .balance(balance)
                .type(type)
                .status(status)
                .lastInterestAccruedDate(lastInterestAccruedDate)
                .build();
    }
}
