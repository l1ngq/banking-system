package com.bank.currencies.service;

import com.bank.common.exception.NotFoundException;
import com.bank.currencies.client.FrankfurterClient;
import com.bank.currencies.entity.ExchangeRateEntity;
import com.bank.currencies.repository.ExchangeRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceUnitTest {

    @Mock
    private ExchangeRateRepository repository;

    @Mock
    private FrankfurterClient frankfurterClient;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @Test
    void getRateReturnsRateFromRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("rate:USD:RUB")).thenReturn("90.00");

        BigDecimal rate = exchangeRateService.getRate("USD", "RUB");

        assertThat(rate).isEqualByComparingTo("90.00");
    }

    @Test
    void getRateReturnsRateFromDatabaseAndCachesIt() {
        ExchangeRateEntity entity = ExchangeRateEntity.builder()
                .id(1L)
                .baseCurrency("USD")
                .targetCurrency("RUB")
                .rate(new BigDecimal("90.00"))
                .updatedAt(Instant.now())
                .build();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("rate:USD:RUB")).thenReturn(null);
        when(repository.findByBaseCurrencyAndTargetCurrency("USD", "RUB"))
                .thenReturn(Optional.of(entity));

        BigDecimal rate = exchangeRateService.getRate("USD", "RUB");

        assertThat(rate).isEqualByComparingTo("90.00");
        verify(valueOperations).set("rate:USD:RUB", entity.getRate(), 55, TimeUnit.MINUTES);
    }

    @Test
    void getRateThrowsNotFoundWhenRateIsMissing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("rate:USD:RUB")).thenReturn(null);
        when(repository.findByBaseCurrencyAndTargetCurrency("USD", "RUB"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> exchangeRateService.getRate("USD", "RUB"))
                .isInstanceOf(NotFoundException.class);
    }
}
