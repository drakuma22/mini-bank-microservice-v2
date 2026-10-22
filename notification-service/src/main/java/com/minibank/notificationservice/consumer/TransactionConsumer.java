package com.minibank.notificationservice.consumer;

import com.minibank.notificationservice.entity.Notification;
import com.minibank.notificationservice.event.TransactionEvent;
import com.minibank.notificationservice.process_message.ProcessedMessage;
import com.minibank.notificationservice.repository.NotificaRepository;
import com.minibank.notificationservice.repository.ProcessedMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionConsumer {

    private final ProcessedMessageRepository processedMessageRepository;
    private final NotificaRepository notificaRepository;
    private final TransactionTemplate transactionTemplate;

    @KafkaListener(topics = "transaction-completed", groupId = "notification-group")
    public void consume(TransactionEvent event, Acknowledgment ack) {
        String id = event.getTransactionId().toString();

        // PASSO 1: già elaborato? Confermo e salto
        if (processedMessageRepository.existsById(id)) {
            log.info("Evento {} già processato, salto", id);
            ack.acknowledge();
            return;
        }

        // PASSO 2 + 3 in un'unica transazione: o entrambi salvati, o nessuno
        transactionTemplate.executeWithoutResult(status -> {
            notificaRepository.save(new Notification(
                    event.getFromAccountNumber(),
                    event.getAmount(),
                    event.getStatus(),
                    LocalDateTime.now()
            ));
            processedMessageRepository.save(new ProcessedMessage(id, LocalDateTime.now(), "TRANSACTION_COMPLETED"));
        });

        // PASSO 4: qui il commit è GIÀ avvenuto. Solo ora confermo a Kafka.
        ack.acknowledge();
    }
}