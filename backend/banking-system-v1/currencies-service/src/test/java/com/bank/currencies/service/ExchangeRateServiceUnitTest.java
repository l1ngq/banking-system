package com.bank.currencies.service;

import com.bank.common.enums.Currency;
import com.bank.common.exception.ConflictException;
import com.bank.common.exception.NotFoundException;
import com.bank.currencies.controller.dto.ConversionResult;
import com.bank.currencies.controller.dto.RateDto;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceUnitTest {

    @Mock
    private ExchangeRateRepository repository;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @Test
    @DisplayName("Одинаковые валюты возвращают курс один")
    void getRateReturnsOneWhenCurrenciesAreEqual() {
        BigDecimal rate = exchangeRateService.getRate("USD", "USD");

        assertThat(rate).isEqualByComparingTo(BigDecimal.ONE);
        verify(repository, never()).findByBaseCurrencyAndTargetCurrency(any(), any());
    }

    @Test
    @DisplayName("Курс берётся из базы, если он есть")
    void getRateReturnsRateFromDatabase() {
        when(repository.findByBaseCurrencyAndTargetCurrency("USD", "RUB"))
                .thenReturn(Optional.of(rate("USD", "RUB", "90.00")));

        BigDecimal rate = exchangeRateService.getRate("USD", "RUB");

        assertThat(rate).isEqualByComparingTo("90.00");
    }

    @Test
    @DisplayName("Если курса нет, выбрасывается NotFoundException")
    void getRateThrowsNotFoundWhenRateIsMissing() {
        when(repository.findByBaseCurrencyAndTargetCurrency("USD", "RUB"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> exchangeRateService.getRate("USD", "RUB"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Конвертация использует курс из базы")
    void convertUsesDatabaseRate() {
        when(repository.findByBaseCurrencyAndTargetCurrency("USD", "RUB"))
                .thenReturn(Optional.of(rate("USD", "RUB", "90.00")));

        ConversionResult result = exchangeRateService.convert("USD", "RUB", new BigDecimal("10.00"));

        assertThat(result.getConvertedAmount()).isEqualByComparingTo("900.00");
        assertThat(result.getRate()).isEqualByComparingTo("90.00");
    }

    @Test
    @DisplayName("upsertRate создаёт новый курс")
    void upsertCreatesNewRate() {
        when(repository.findByBaseCurrencyAndTargetCurrency("USD", "RUB"))
                .thenReturn(Optional.empty());
        when(repository.save(any(ExchangeRateEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RateDto result = exchangeRateService.upsertRate(Currency.USD, Currency.RUB, new BigDecimal("90.00"));

        ArgumentCaptor<ExchangeRateEntity> captor = ArgumentCaptor.forClass(ExchangeRateEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getBaseCurrency()).isEqualTo("USD");
        assertThat(captor.getValue().getTargetCurrency()).isEqualTo("RUB");
        assertThat(captor.getValue().getRate()).isEqualByComparingTo("90.00");
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
        assertThat(result.getRate()).isEqualByComparingTo("90.00");
    }

    @Test
    @DisplayName("upsertRate обновляет существующий курс")
    void upsertUpdatesExistingRate() {
        Instant oldUpdatedAt = Instant.parse("2026-05-01T00:00:00Z");
        ExchangeRateEntity existing = ExchangeRateEntity.builder()
                .id(1L)
                .baseCurrency("USD")
                .targetCurrency("RUB")
                .rate(new BigDecimal("85.00"))
                .updatedAt(oldUpdatedAt)
                .build();
        when(repository.findByBaseCurrencyAndTargetCurrency("USD", "RUB"))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(ExchangeRateEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RateDto result = exchangeRateService.upsertRate(Currency.USD, Currency.RUB, new BigDecimal("90.00"));

        verify(repository).save(existing);
        assertThat(existing.getRate()).isEqualByComparingTo("90.00");
        assertThat(existing.getUpdatedAt()).isAfter(oldUpdatedAt);
        assertThat(result.getRate()).isEqualByComparingTo("90.00");
    }

    @Test
    @DisplayName("upsertRate отклоняет невалидный курс")
    void upsertRejectsInvalidRate() {
        assertThatThrownBy(() -> exchangeRateService.upsertRate(Currency.USD, Currency.RUB, BigDecimal.ZERO))
                .isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("upsertRate запрещает одинаковые валюты")
    void upsertRejectsSameCurrencyPair() {
        assertThatThrownBy(() -> exchangeRateService.upsertRate(Currency.USD, Currency.USD, BigDecimal.ONE))
                .isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
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
