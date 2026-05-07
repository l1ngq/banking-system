package com.bank.core.service;

import com.bank.common.event.InterestAccrualEvent;
import com.bank.core.entity.BankAccountEntity;
import com.bank.core.entity.InterestAccrualLogEntity;
import com.bank.core.kafka.producer.InterestEventProducer;
import com.bank.core.repository.BankAccountRepository;
import com.bank.core.repository.InterestAccrualLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Слой бизнес логики начисления процентов
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterestAccrualService {

    private static final BigDecimal ANNUAL_RATE = new BigDecimal("0.05");
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    private final BankAccountRepository bankAccountRepository;
    private final InterestAccrualLogRepository interestAccrualLogRepository;
    private final InterestEventProducer interestEventProducer;

    @Lazy
    @Autowired
    private InterestAccrualService self;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void accrueForAccount(BankAccountEntity account) {
        log.info("Начисляем проценты для счёта id={}", account.getId());

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        if (today.equals(account.getLastInterestAccruedDate())) {
            log.info("Проценты уже начислены сегодня для счёта {}", account.getId());
            return;
        }

        BigDecimal dailyInterest = account.getBalance()
                .multiply(ANNUAL_RATE)
                .divide(DAYS_IN_YEAR, 2, RoundingMode.HALF_UP);

        if (dailyInterest.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        account.setBalance(account.getBalance().add(dailyInterest));
        account.setLastInterestAccruedDate(today);
        bankAccountRepository.save(account);

        InterestAccrualLogEntity logEntry = InterestAccrualLogEntity.builder()
                .accountId(account.getId())
                .userId(account.getUserId())
                .amount(dailyInterest)
                .accruedDate(today)
                .build();
        interestAccrualLogRepository.save(logEntry);
        interestEventProducer.send(InterestAccrualEvent.builder()
                .accountId(account.getId())
                .userId(account.getUserId())
                .amount(dailyInterest)
                .accruedDate(today)
                .timestamp(Instant.now())
                .build());

        log.info("Начислено {} для счёта {}. Новый баланс: {}",
                dailyInterest, account.getId(), account.getBalance());
    }

    @Transactional(readOnly = true)
    public void runAccrualForAll() {
        log.info("=== Запуск начисления процентов ===");
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int page = 0;
        int pageSize = 100;
        int processed = 0;
        List<BankAccountEntity> accounts = new ArrayList<>();

        while (true) {
            Page<BankAccountEntity> batch = bankAccountRepository
                    .findSavingsForAccrual(today, PageRequest.of(page, pageSize));
            if (batch.isEmpty()) {
                break;
            }

            accounts.addAll(batch.getContent());

            if (!batch.hasNext()) {
                break;
            }
            page++;
        }

        for (BankAccountEntity account : accounts) {
            try {
                self.accrueForAccount(account);
                processed++;
            } catch (Exception e) {
                log.error("Ошибка начисления для счёта {}: {}", account.getId(), e.getMessage());
            }
        }

        log.info("=== Начисление завершено. Обработано счетов: {} ===", processed);
    }
}
