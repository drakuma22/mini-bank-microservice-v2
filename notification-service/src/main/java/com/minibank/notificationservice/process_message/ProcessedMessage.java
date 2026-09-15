package com.minibank.notificationservice.process_message;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_messages")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedMessage {
    @Id
    private String messageId;
    private LocalDateTime processedAt;
    private String eventType;
}
