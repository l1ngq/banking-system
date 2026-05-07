package com.bank.core.dto;

import com.bank.common.enums.Currency;
import com.bank.common.enums.TransactionStatus;
import com.bank.common.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {

    private UUID id;
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
    private BigDecimal convertedAmount;
    private Currency currency;
    private TransactionType type;
    private TransactionStatus status;
    private Instant createdAt;
}
