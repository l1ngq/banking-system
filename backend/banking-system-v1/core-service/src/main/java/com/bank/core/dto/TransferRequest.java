package com.bank.core.dto;

import com.bank.common.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class TransferRequest {

    @NotNull
    private Long fromAccountId;

    @NotBlank
    @Size(min = 20, max = 20)
    private String toAccountNumber;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull
    private Currency currency;

    public TransferRequest(Long fromAccountId, String toAccountNumber, BigDecimal amount, Currency currency) {
        this.fromAccountId = fromAccountId;
        setToAccountNumber(toAccountNumber);
        this.amount = amount;
        this.currency = currency;
    }

    public void setToAccountNumber(String toAccountNumber) {
        this.toAccountNumber = normalizeAccountNumber(toAccountNumber);
    }

    private String normalizeAccountNumber(String accountNumber) {
        return accountNumber == null ? null : accountNumber.replaceAll("\\s+", "");
    }
}
