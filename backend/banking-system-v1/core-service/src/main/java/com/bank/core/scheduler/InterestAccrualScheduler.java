package com.bank.core.scheduler;

import com.bank.core.service.InterestAccrualService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterestAccrualScheduler {

    private final InterestAccrualService interestAccrualService;

    @Scheduled(cron = "0 0 1 * * *", zone = "UTC")
    public void scheduledAccrual() {
        log.info("Scheduler запускает начисление процентов");
        interestAccrualService.runAccrualForAll();
    }
}
