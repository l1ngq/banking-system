package com.bank.currencies.service;

import com.bank.common.exception.NotFoundException;
import com.bank.currencies.client.FrankfurterClient;
import com.bank.currencies.controller.dto.ConversionResult;
import com.bank.currencies.entity.ExchangeRateEntity;
import com.bank.currencies.repository.ExchangeRateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceUnitTest {

    @Mock
    private ExchangeRateRepository repository;

    @Mock
    private FrankfurterClient frankfurterClient;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @Test
    @DisplayName("Одинаковые валюты возвращают курс один")
    void getRateReturnsOneWhenCurrenciesAreEqual() {
        // when
        BigDecimal rate = exchangeRateService.getRate("USD", "USD");

        // then
        assertThat(rate).isEqualByComparingTo(BigDecimal.ONE);
        verify(repository, never()).findByBaseCurrencyAndTargetCurrency(any(), any());
    }

    @Test
    @DisplayName("Курс берётся из базы, если он есть")
    void getRateReturnsRateFromDatabase() {
        ExchangeRateEntity entity = ExchangeRateEntity.builder()
                .id(1L)
                .baseCurrency("USD")
                .targetCurrency("RUB")
                .rate(new BigDecimal("90.00"))
                .updatedAt(Instant.now())
                .build();
        when(repository.findByBaseCurrencyAndTargetCurrency("USD", "RUB"))
                .thenReturn(Optional.of(entity));

        // when
        BigDecimal rate = exchangeRateService.getRate("USD", "RUB");

        // then
        assertThat(rate).isEqualByComparingTo("90.00");
    }

    @Test
    @DisplayName("Если курса нет, выбрасывается NotFoundException")
    void getRateThrowsNotFoundWhenRateIsMissing() {
        when(repository.findByBaseCurrencyAndTargetCurrency("USD", "RUB"))
                .thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> exchangeRateService.getRate("USD", "RUB"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Конвертация округляет сумму до 2 знаков")
    void convertRoundsConvertedAmountToTwoDigits() {
        when(repository.findByBaseCurrencyAndTargetCurrency("USD", "RUB"))
                .thenReturn(Optional.of(ExchangeRateEntity.builder()
                        .baseCurrency("USD")
                        .targetCurrency("RUB")
                        .rate(new BigDecimal("1.005"))
                        .updatedAt(Instant.now())
                        .build()));

        // when
        ConversionResult result = exchangeRateService.convert("USD", "RUB", new BigDecimal("1.00"));

        // then
        assertThat(result.getConvertedAmount()).isEqualByComparingTo("1.01");
    }

    @Test
    @DisplayName("Обновление курсов изменяет существующую запись")
    void refreshRatesUpdatesExistingRate() {
        ExchangeRateEntity existing = ExchangeRateEntity.builder()
                .id(1L)
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("0.80"))
                .updatedAt(Instant.parse("2026-05-01T00:00:00Z"))
                .build();
        when(frankfurterClient.fetchRates("USD", List.of("EUR", "RUB")))
                .thenReturn(Map.of("EUR", new BigDecimal("0.90")));
        when(frankfurterClient.fetchRates("EUR", List.of("USD", "RUB")))
                .thenReturn(Map.of());
        when(frankfurterClient.fetchRates("RUB", List.of("USD", "EUR")))
                .thenReturn(Map.of());
        when(repository.findByBaseCurrencyAndTargetCurrency("USD", "EUR"))
                .thenReturn(Optional.of(existing));

        // when
        exchangeRateService.refreshRates();

        // then
        ArgumentCaptor<ExchangeRateEntity> captor = ArgumentCaptor.forClass(ExchangeRateEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
        assertThat(existing.getRate()).isEqualByComparingTo("0.90");
        assertThat(existing.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Пустой ответ FrankfurterClient не ломает обновление")
    void refreshRatesDoesNotFailWhenFrankfurterReturnsEmptyRates() {
        when(frankfurterClient.fetchRates("USD", List.of("EUR", "RUB"))).thenReturn(Map.of());
        when(frankfurterClient.fetchRates("EUR", List.of("USD", "RUB"))).thenReturn(Map.of());
        when(frankfurterClient.fetchRates("RUB", List.of("USD", "EUR"))).thenReturn(Map.of());

        // when / then
        assertThatCode(() -> exchangeRateService.refreshRates()).doesNotThrowAnyException();
        verify(repository, never()).save(any());
    }
}
