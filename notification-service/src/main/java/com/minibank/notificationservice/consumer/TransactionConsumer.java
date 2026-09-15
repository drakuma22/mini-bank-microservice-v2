package com.minibank.notificationservice.consumer;

import com.minibank.notificationservice.entity.Notification;
import com.minibank.notificationservice.event.TransactionEvent;
import com.minibank.notificationservice.process_message.ProcessedMessage;
import com.minibank.notificationservice.repository.NotificaRepository;
import com.minibank.notificationservice.repository.ProcessedMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Slf4j
public class TransactionConsumer {

    @Autowired
    private ProcessedMessageRepository processedMessageRepository;

    @Autowired
    private NotificaRepository notificaRepository;

    @KafkaListener(topics = "transaction-completed", groupId = "notification-group")
    @Transactional
    public void consume(TransactionEvent event, Acknowledgment ack) {
        String id = event.getTransactionId().toString();

        // PASSO 1: controllo — questo id è già stato gestito?
        if (processedMessageRepository.existsById(id)) {
            log.info("Evento {} già processato, salto", id);
            ack.acknowledge();
            return;
        }

        // PASSO 2: eseguo la logica di business reale
        Notification notifica = new Notification(
                event.getFromAccountNumber(),
                event.getAmount(),
                event.getStatus(),
                LocalDateTime.now()
        );
        notificaRepository.save(notifica);

        // PASSO 3: registro l'id — STESSA transazione del passo 2
        processedMessageRepository.save(new ProcessedMessage(id, LocalDateTime.now(), "TRANSACTION_COMPLETED"));

        ack.acknowledge();
    }
}