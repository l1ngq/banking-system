package com.bank.currencies.service;

import com.bank.common.exception.NotFoundException;
import com.bank.currencies.client.FrankfurterClient;
import com.bank.currencies.controller.dto.ConversionResult;
import com.bank.currencies.entity.ExchangeRateEntity;
import com.bank.currencies.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final long REDIS_TTL_MINUTES = 55;
    private static final List<String> SUPPORTED = List.of("USD", "EUR", "RUB");

    private final ExchangeRateRepository repository;
    private final FrankfurterClient frankfurterClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public void refreshRates() {
        log.info("Обновляем курсы валют из Frankfurter API");
        for (String base : SUPPORTED) {
            List<String> targets = SUPPORTED.stream()
                    .filter(currency -> !currency.equals(base))
                    .toList();
            Map<String, BigDecimal> rates = frankfurterClient.fetchRates(base, targets);
            for (Map.Entry<String, BigDecimal> entry : rates.entrySet()) {
                saveAndCache(base, entry.getKey(), entry.getValue());
            }
        }
    }

    private void saveAndCache(String from, String to, BigDecimal rate) {
        ExchangeRateEntity entity = repository
                .findByBaseCurrencyAndTargetCurrency(from, to)
                .orElse(ExchangeRateEntity.builder()
                        .baseCurrency(from)
                        .targetCurrency(to)
                        .build());

        entity.setRate(rate);
        entity.setUpdatedAt(Instant.now());
        repository.save(entity);

        String key = "rate:" + from + ":" + to;
        redisTemplate.opsForValue().set(key, rate, REDIS_TTL_MINUTES, TimeUnit.MINUTES);
        log.debug("Кэширован курс {}→{} = {} (TTL=55m)", from, to, rate);
    }

    public BigDecimal getRate(String from, String to) {
        log.info("Request to get rate from {} to {}", from, to);
        if (from.equals(to)) {
            return BigDecimal.ONE;
        }

        String key = "rate:" + from + ":" + to;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.debug("Курс {}→{} из Redis: {}", from, to, cached);
            return new BigDecimal(cached.toString());
        }

        ExchangeRateEntity entity = repository
                .findByBaseCurrencyAndTargetCurrency(from, to)
                .orElseThrow(() -> new NotFoundException("Курс не найден: " + from + "→" + to));

        redisTemplate.opsForValue().set(key, entity.getRate(), REDIS_TTL_MINUTES, TimeUnit.MINUTES);
        return entity.getRate();
    }

    public ConversionResult convert(String from, String to, BigDecimal amount) {
        log.info("Request to convert {} from {} to {}", amount, from, to);
        BigDecimal rate = getRate(from, to);
        BigDecimal converted = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        return new ConversionResult(from, to, amount, converted, rate, Instant.now());
    }
}
