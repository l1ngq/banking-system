package com.bank.currencies.integration;

import com.bank.currencies.entity.ExchangeRateEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrenciesRepositoryIntegrationTest extends BaseCurrenciesPostgresIntegrationTest {

    @Test
    @DisplayName("Spring Context currencies-service загружается")
    void contextLoads() {
        assertThat(exchangeRateRepository).isNotNull();
    }

    @Test
    @DisplayName("ExchangeRateRepository сохраняет курс валют")
    void exchangeRateRepositorySavesRate() {
        ExchangeRateEntity saved = exchangeRateRepository.save(rate("USD", "RUB", "90.123456"));

        assertThat(saved.getId()).isNotNull();
        assertThat(exchangeRateRepository.findById(saved.getId()))
                .get()
                .extracting(ExchangeRateEntity::getRate)
                .isEqualTo(new BigDecimal("90.123456"));
    }

    @Test
    @DisplayName("ExchangeRateRepository находит курс по валютной паре")
    void findByBaseCurrencyAndTargetCurrencyFindsSavedRate() {
        exchangeRateRepository.save(rate("EUR", "USD", "1.080000"));

        assertThat(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency("EUR", "USD"))
                .get()
                .extracting(ExchangeRateEntity::getRate)
                .isEqualTo(new BigDecimal("1.080000"));
    }

    @Test
    @DisplayName("ExchangeRateRepository запрещает дубликат валютной пары")
    void exchangeRateRepositoryRejectsDuplicatePair() {
        exchangeRateRepository.saveAndFlush(rate("USD", "EUR", "0.910000"));

        assertThatThrownBy(() -> exchangeRateRepository.saveAndFlush(rate("USD", "EUR", "0.920000")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static ExchangeRateEntity rate(String baseCurrency, String targetCurrency, String rate) {
        return ExchangeRateEntity.builder()
                .baseCurrency(baseCurrency)
                .targetCurrency(targetCurrency)
                .rate(new BigDecimal(rate))
                .updatedAt(Instant.now())
                .build();
    }
}
