package com.bank.currencies;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(excludeName = "org.springframework.cloud.openfeign.FeignAutoConfiguration")
@EnableScheduling
public class CurrenciesServiceApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(CurrenciesServiceApplication.class, args);
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);
            throw throwable;
        }
    }
}
