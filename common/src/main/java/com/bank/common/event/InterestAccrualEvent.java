package com.bank.common.event;

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
public class InterestAccrualEvent {

    public static final String TOPIC = "interest-events";

    private UUID accountId;
    private UUID userId;
    private BigDecimal amount;
    private LocalDate accruedDate;
    private Instant timestamp;
}
