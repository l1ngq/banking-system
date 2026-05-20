package com.bank.core.kafka.producer;

import com.bank.common.event.InterestAccrualEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterestEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(InterestAccrualEvent event) {
        try {
            var meta = kafkaTemplate
                    .send(InterestAccrualEvent.TOPIC, event.getAccountId().toString(), event)
                    .get()
                    .getRecordMetadata();
            log.info("--> Kafka [{}] partition={} offset={}: {}",
                    meta.topic(), meta.partition(), meta.offset(), event);
        } catch (Exception e) {
            log.error("Ошибка отправки события начисления процентов в Kafka", e);
        }
    }
}
