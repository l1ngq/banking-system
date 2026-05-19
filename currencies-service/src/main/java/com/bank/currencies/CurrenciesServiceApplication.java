package com.bank.currencies;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
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
