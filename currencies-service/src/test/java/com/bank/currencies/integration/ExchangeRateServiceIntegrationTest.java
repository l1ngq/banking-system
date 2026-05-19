package com.bank.currencies.integration;

import com.bank.common.enums.Currency;
import com.bank.common.exception.NotFoundException;
import com.bank.currencies.controller.dto.ConversionResult;
import com.bank.currencies.controller.dto.RateDto;
import com.bank.currencies.entity.ExchangeRateEntity;
import com.bank.currencies.service.ExchangeRateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeRateServiceIntegrationTest extends BaseCurrenciesPostgresIntegrationTest {

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Test
    @DisplayName("getRate берёт курс из БД")
    void getRateReturnsRateFromDatabase() {
        exchangeRateRepository.save(rate("USD", "RUB", "90.500000"));

        BigDecimal result = exchangeRateService.getRate("USD", "RUB");

        assertThat(result).isEqualByComparingTo("90.500000");
    }

    @Test
    @DisplayName("getRate для одинаковых валют возвращает один")
    void getRateReturnsOneForSameCurrency() {
        BigDecimal result = exchangeRateService.getRate("USD", "USD");

        assertThat(result).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("getRate выбрасывает NotFoundException, если курса нет")
    void getRateThrowsNotFoundWhenRateDoesNotExist() {
        assertThatThrownBy(() -> exchangeRateService.getRate("USD", "RUB"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("convert корректно считает сумму и округляет до двух знаков")
    void convertCalculatesConvertedAmountAndRoundsToTwoDigits() {
        exchangeRateRepository.save(rate("USD", "RUB", "1.005000"));

        ConversionResult result = exchangeRateService.convert("USD", "RUB", new BigDecimal("1.00"));

        assertThat(result.getConvertedAmount()).isEqualByComparingTo("1.01");
        assertThat(result.getRate()).isEqualByComparingTo("1.005000");
    }

    @Test
    @DisplayName("upsertRate создаёт новый курс")
    void upsertRateCreatesNewRate() {
        RateDto result = exchangeRateService.upsertRate(Currency.USD, Currency.RUB, new BigDecimal("90.000000"));

        assertThat(result.getBaseCurrency()).isEqualTo("USD");
        assertThat(result.getTargetCurrency()).isEqualTo("RUB");
        assertThat(result.getRate()).isEqualByComparingTo("90.000000");
        assertThat(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency("USD", "RUB"))
                .get()
                .extracting(ExchangeRateEntity::getRate)
                .isEqualTo(new BigDecimal("90.000000"));
    }

    @Test
    @DisplayName("upsertRate обновляет существующий курс")
    void upsertRateUpdatesExistingRate() {
        ExchangeRateEntity existing = exchangeRateRepository.save(rate("USD", "RUB", "85.000000"));

        RateDto result = exchangeRateService.upsertRate(Currency.USD, Currency.RUB, new BigDecimal("90.000000"));

        assertThat(result.getRate()).isEqualByComparingTo("90.000000");
        assertThat(exchangeRateRepository.findById(existing.getId()))
                .get()
                .extracting(ExchangeRateEntity::getRate)
                .isEqualTo(new BigDecimal("90.000000"));
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
