package com.bank.core.dto;

import com.bank.common.enums.AccountType;
import com.bank.common.enums.Currency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    public CreateAccountRequest(Currency currency, AccountType type) {
        this(currency, type, null);
    }

    @NotNull(message = "Валюта не может быть null")
    private Currency currency;

    @NotNull(message = "Тип счёта не может быть null")
    private AccountType type;

    @Size(max = 40, message = "Название счёта не может быть длиннее 40 символов")
    private String displayName;
}
