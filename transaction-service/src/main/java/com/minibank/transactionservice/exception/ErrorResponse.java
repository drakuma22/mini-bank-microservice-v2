package com.minibank.transactionservice.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        String message,
        String errorCode,
        LocalDateTime timestamp
) {
    public ErrorResponse(String message, String errorCode) {
        this(message, errorCode, LocalDateTime.now());
    }
}