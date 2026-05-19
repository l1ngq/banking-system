package com.bank.core.kafka.consumer;

import com.bank.common.event.InterestAccrualEvent;
import com.bank.common.event.TransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationConsumer {

    @KafkaListener(
            topics = TransactionEvent.TOPIC,
            groupId = "core-notification-group"
    )
    public void listenTransactions(
            @Payload TransactionEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
            @Header(KafkaHeaders.OFFSET) Long offset) {
        log.info("<-- Kafka [topic={}, partition={}, offset={}] TransactionEvent: {}",
                topic, partition, offset, event);
    }

    @KafkaListener(
            topics = InterestAccrualEvent.TOPIC,
            groupId = "core-interest-notification-group"
    )
    public void listenInterest(
            @Payload InterestAccrualEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
            @Header(KafkaHeaders.OFFSET) Long offset) {
        log.info("<-- Kafka [topic={}, partition={}, offset={}] InterestEvent: {}",
                topic, partition, offset, event);
    }
}
