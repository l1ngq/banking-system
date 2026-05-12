package com.bank.currencies.integration;

import com.bank.common.exception.NotFoundException;
import com.bank.currencies.controller.dto.ConversionResult;
import com.bank.currencies.entity.ExchangeRateEntity;
import com.bank.currencies.service.ExchangeRateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExchangeRateServiceIntegrationTest extends BaseCurrenciesPostgresIntegrationTest {

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Test
    @DisplayName("getRate берёт курс из БД при промахе Redis")
    void getRateReturnsRateFromDatabaseWhenRedisMisses() {
        exchangeRateRepository.save(rate("USD", "RUB", "90.500000"));
        when(valueOperations.get("rate:USD:RUB")).thenReturn(null);

        BigDecimal result = exchangeRateService.getRate("USD", "RUB");

        assertThat(result).isEqualByComparingTo("90.500000");
        verify(valueOperations).set("rate:USD:RUB", new BigDecimal("90.500000"), 55L, TimeUnit.MINUTES);
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
        when(valueOperations.get("rate:USD:RUB")).thenReturn(null);

        assertThatThrownBy(() -> exchangeRateService.getRate("USD", "RUB"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("convert корректно считает сумму и округляет до двух знаков")
    void convertCalculatesConvertedAmountAndRoundsToTwoDigits() {
        exchangeRateRepository.save(rate("USD", "RUB", "1.005000"));
        when(valueOperations.get("rate:USD:RUB")).thenReturn(null);

        ConversionResult result = exchangeRateService.convert("USD", "RUB", new BigDecimal("1.00"));

        assertThat(result.getConvertedAmount()).isEqualByComparingTo("1.01");
        assertThat(result.getRate()).isEqualByComparingTo("1.005000");
    }

    @Test
    @DisplayName("refreshRates сохраняет новые курсы из FrankfurterClient")
    void refreshRatesSavesNewRatesFromFrankfurterClient() {
        when(frankfurterClient.fetchRates("USD", List.of("EUR", "RUB")))
                .thenReturn(Map.of("EUR", new BigDecimal("0.910000")));
        when(frankfurterClient.fetchRates("EUR", List.of("USD", "RUB")))
                .thenReturn(Map.of("USD", new BigDecimal("1.100000")));
        when(frankfurterClient.fetchRates("RUB", List.of("USD", "EUR")))
                .thenReturn(Map.of());

        exchangeRateService.refreshRates();

        assertThat(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency("USD", "EUR"))
                .get()
                .extracting(ExchangeRateEntity::getRate)
                .isEqualTo(new BigDecimal("0.910000"));
        assertThat(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency("EUR", "USD"))
                .get()
                .extracting(ExchangeRateEntity::getRate)
                .isEqualTo(new BigDecimal("1.100000"));
    }

    @Test
    @DisplayName("refreshRates обновляет существующий курс")
    void refreshRatesUpdatesExistingRate() {
        ExchangeRateEntity existing = exchangeRateRepository.save(rate("USD", "EUR", "0.800000"));
        when(frankfurterClient.fetchRates("USD", List.of("EUR", "RUB")))
                .thenReturn(Map.of("EUR", new BigDecimal("0.920000")));
        when(frankfurterClient.fetchRates("EUR", List.of("USD", "RUB")))
                .thenReturn(Map.of());
        when(frankfurterClient.fetchRates("RUB", List.of("USD", "EUR")))
                .thenReturn(Map.of());

        exchangeRateService.refreshRates();

        assertThat(exchangeRateRepository.findById(existing.getId()))
                .get()
                .extracting(ExchangeRateEntity::getRate)
                .isEqualTo(new BigDecimal("0.920000"));
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
