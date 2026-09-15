package com.minibank.transactionservice.producer;

import com.minibank.transactionservice.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionProducer {

    private static final String TOPIC = "transaction-completed";
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public void publish(TransactionEvent event) {
        log.info("Publishing event to topic {}: {}", TOPIC, event);
        kafkaTemplate.send(TOPIC, event.getFromAccountNumber(), event);
    }
}