package com.bank.core.dto;

import com.bank.common.enums.AccountType;
import com.bank.common.enums.Currency;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    @NotNull(message = "Валюта не может быть null")
    private Currency currency;

    @NotNull(message = "Тип счёта не может быть null")
    private AccountType type;
}
