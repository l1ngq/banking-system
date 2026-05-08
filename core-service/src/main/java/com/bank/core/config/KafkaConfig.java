package com.bank.core.config;

import com.bank.common.event.InterestAccrualEvent;
import com.bank.common.event.TransactionEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    @SuppressWarnings({"rawtypes", "unchecked"})
    public KafkaTemplate<String, TransactionEvent> transactionKafkaTemplate(
            ProducerFactory<Object, Object> producerFactory) {
        return new KafkaTemplate(producerFactory);
    }

    @Bean
    @SuppressWarnings({"rawtypes", "unchecked"})
    public KafkaTemplate<String, InterestAccrualEvent> interestKafkaTemplate(
            ProducerFactory<Object, Object> producerFactory) {
        return new KafkaTemplate(producerFactory);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Object>>
            kafkaListenerContainerFactory(Environment environment) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(kafkaConsumerFactory(environment));
        factory.setAutoStartup(environment.getProperty(
                "spring.kafka.listener.auto-startup", Boolean.class, true));
        return factory;
    }

    private ConsumerFactory<String, Object> kafkaConsumerFactory(Environment environment) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                environment.getProperty("spring.kafka.bootstrap-servers"));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(props);
    }
}
