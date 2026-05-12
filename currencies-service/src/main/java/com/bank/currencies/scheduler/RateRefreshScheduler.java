package com.bank.currencies.scheduler;

import com.bank.currencies.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateRefreshScheduler {

    private final ExchangeRateService exchangeRateService;

    @Scheduled(cron = "0 0 * * * *", zone = "UTC")
    public void refresh() {
        log.info("Request to refresh exchange rates by schedule");
        exchangeRateService.refreshRates();
    }
}
