package com.bank.currencies.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateDto {

    private String baseCurrency;
    private String targetCurrency;
    private BigDecimal rate;
    private Instant updatedAt;
}
