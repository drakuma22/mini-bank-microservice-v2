package com.minibank.notificationservice.repository;

import com.minibank.notificationservice.process_message.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {
}
