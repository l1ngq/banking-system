package com.bank.core.dto;

import com.bank.common.enums.AccountStatus;
import com.bank.common.enums.AccountType;
import com.bank.common.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {

    private Long id;
    private UUID userId;
    private Currency currency;
    private BigDecimal balance;
    private AccountType type;
    private AccountStatus status;
    private LocalDate lastInterestAccruedDate;
    private Instant createdAt;
}
