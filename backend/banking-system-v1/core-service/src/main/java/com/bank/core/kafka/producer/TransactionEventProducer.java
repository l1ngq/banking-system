package com.bank.core.kafka.producer;

import com.bank.common.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(TransactionEvent event) {
        try {
            var meta = kafkaTemplate
                    .send(TransactionEvent.TOPIC, event.getUserId().toString(), event)
                    .get()
                    .getRecordMetadata();
            log.info("--> Kafka [{}] partition={} offset={}: {}",
                    meta.topic(), meta.partition(), meta.offset(), event);
        } catch (Exception e) {
            log.error("Ошибка отправки события транзакции в Kafka", e);
        }
    }
}
