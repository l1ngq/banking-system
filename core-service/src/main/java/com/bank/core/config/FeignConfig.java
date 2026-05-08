package com.bank.core.config;

import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class FeignConfig {

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                2000,
                TimeUnit.MILLISECONDS,
                3000,
                TimeUnit.MILLISECONDS,
                true);
    }
}
