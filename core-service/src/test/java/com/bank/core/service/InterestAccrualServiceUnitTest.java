package com.bank.core.service;

import com.bank.common.enums.AccountStatus;
import com.bank.common.enums.AccountType;
import com.bank.common.enums.Currency;
import com.bank.core.entity.BankAccountEntity;
import com.bank.core.kafka.producer.InterestEventProducer;
import com.bank.core.repository.BankAccountRepository;
import com.bank.core.repository.InterestAccrualLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    void accrueForAccountDoesNothingWhenAlreadyAccruedToday() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        BankAccountEntity account = savingsAccount(new BigDecimal("10000.00"), today);

        interestAccrualService.accrueForAccount(account);

        assertThat(account.getBalance()).isEqualByComparingTo("10000.00");
        verify(bankAccountRepository, never()).save(any());
        verify(interestAccrualLogRepository, never()).save(any());
    }

    private BankAccountEntity savingsAccount(BigDecimal balance, LocalDate lastInterestAccruedDate) {
        return BankAccountEntity.builder()
                .id(1L)
                .userId(UUID.randomUUID())
                .currency(Currency.USD)
                .balance(balance)
                .type(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .lastInterestAccruedDate(lastInterestAccruedDate)
                .build();
    }
}
