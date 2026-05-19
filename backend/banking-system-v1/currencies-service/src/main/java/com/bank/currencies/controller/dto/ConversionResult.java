package com.bank.currencies.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversionResult {

    private String from;
    private String to;
    private BigDecimal amount;
    private BigDecimal convertedAmount;
    private BigDecimal rate;
    private Instant timestamp;
}
