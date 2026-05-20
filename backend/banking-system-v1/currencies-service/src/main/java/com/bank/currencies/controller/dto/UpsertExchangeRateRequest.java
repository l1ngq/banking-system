package com.bank.currencies.controller.dto;

import com.bank.common.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpsertExchangeRateRequest {

    @NotNull
    private Currency baseCurrency;

    @NotNull
    private Currency targetCurrency;

    @NotNull
    @DecimalMin(value = "0.000001")
    private BigDecimal rate;
}
