package com.bank.currencies.controller;

import com.bank.common.dto.UniversalResponse;
import com.bank.currencies.controller.dto.ConversionResult;
import com.bank.currencies.controller.dto.RateDto;
import com.bank.currencies.controller.dto.UpsertExchangeRateRequest;
import com.bank.currencies.service.ExchangeRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/currencies")
public class CurrencyController {

    private final ExchangeRateService service;

    @GetMapping("/rate")
    public UniversalResponse<RateDto> getRate(
            @RequestParam("from") String from,
            @RequestParam("to") String to) {
        log.info("Request to get currency rate from {} to {}", from, to);
        BigDecimal rate = service.getRate(from, to);
        return new UniversalResponse<>(new RateDto(from, to, rate, Instant.now()));
    }

    @GetMapping("/convert")
    public UniversalResponse<ConversionResult> convert(
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            @RequestParam("amount") BigDecimal amount) {
        log.info("Request to convert {} from {} to {}", amount, from, to);
        return new UniversalResponse<>(service.convert(from, to, amount));
    }

    @PutMapping("/rates")
    public UniversalResponse<RateDto> upsertRate(
            @Valid @RequestBody UpsertExchangeRateRequest request) {
        log.info("Request to upsert currency rate: {}", request);
        return new UniversalResponse<>(service.upsertRate(
                request.getBaseCurrency(),
                request.getTargetCurrency(),
                request.getRate()
        ));
    }
}
