package com.minibank.notificationservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String transactionStatus;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    public Notification(String fromAccountNumber, BigDecimal amount, String status, LocalDateTime now) {
        this.accountNumber = fromAccountNumber;
        this.amount = amount;
        this.transactionStatus = status;
        this.sentAt = now;
    }
}