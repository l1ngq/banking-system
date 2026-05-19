package com.bank.currencies.service;

import com.bank.common.enums.Currency;
import com.bank.common.exception.ConflictException;
import com.bank.common.exception.NotFoundException;
import com.bank.currencies.controller.dto.ConversionResult;
import com.bank.currencies.controller.dto.RateDto;
import com.bank.currencies.entity.ExchangeRateEntity;
import com.bank.currencies.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository repository;

    public BigDecimal getRate(String from, String to) {
        log.info("Request to get rate from {} to {}", from, to);
        if (from == null || to == null) {
            throw new ConflictException("Currencies must not be null");
        }
        if (from.equals(to)) {
            return BigDecimal.ONE;
        }

        ExchangeRateEntity entity = repository
                .findByBaseCurrencyAndTargetCurrency(from, to)
                .orElseThrow(() -> new NotFoundException("Курс не найден: " + from + "→" + to));

        return entity.getRate();
    }

    public ConversionResult convert(String from, String to, BigDecimal amount) {
        log.info("Request to convert {} from {} to {}", amount, from, to);
        validatePositiveAmount(amount);
        BigDecimal rate = getRate(from, to);
        BigDecimal converted = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        return new ConversionResult(from, to, amount, converted, rate, Instant.now());
    }

    @Transactional
    public RateDto upsertRate(Currency baseCurrency, Currency targetCurrency, BigDecimal rate) {
        log.info("Request to upsert rate from {} to {}", baseCurrency, targetCurrency);
        validateRate(baseCurrency, targetCurrency, rate);

        String base = baseCurrency.name();
        String target = targetCurrency.name();
        ExchangeRateEntity entity = repository.findByBaseCurrencyAndTargetCurrency(base, target)
                .orElseGet(() -> ExchangeRateEntity.builder()
                        .baseCurrency(base)
                        .targetCurrency(target)
                        .build());

        entity.setRate(rate);
        entity.setUpdatedAt(Instant.now());

        ExchangeRateEntity saved = repository.save(entity);
        return new RateDto(
                saved.getBaseCurrency(),
                saved.getTargetCurrency(),
                saved.getRate(),
                saved.getUpdatedAt()
        );
    }

    private void validateRate(Currency baseCurrency, Currency targetCurrency, BigDecimal rate) {
        if (baseCurrency == null || targetCurrency == null) {
            throw new ConflictException("Currencies must not be null");
        }
        if (baseCurrency == targetCurrency) {
            throw new ConflictException("Base and target currencies must be different");
        }
        validatePositiveAmount(rate);
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ConflictException("Amount must be greater than zero");
        }
    }
}
