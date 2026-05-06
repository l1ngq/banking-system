package com.bank.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {

    public static final String TOPIC = "transaction-events";

    private UUID transactionId;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    private String type;
    private Instant timestamp;
}
