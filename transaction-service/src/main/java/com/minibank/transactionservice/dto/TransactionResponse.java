package com.minibank.transactionservice.dto;

import com.minibank.transactionservice.entity.Transaction;
import com.minibank.transactionservice.enums.TransactionStatus;
import com.minibank.transactionservice.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount,
        TransactionType type,
        TransactionStatus status,
        String failureReason,
        LocalDateTime createdAt
) {
    // Conversione entità → DTO in un solo posto
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId(), t.getFromAccountNumber(), t.getToAccountNumber(), t.getAmount(),
                t.getType(), t.getStatus(), t.getFailureReason(), t.getCreatedAt());
    }
}
