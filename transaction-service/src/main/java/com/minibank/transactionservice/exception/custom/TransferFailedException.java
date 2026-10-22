package com.minibank.transactionservice.exception.custom;

public class TransferFailedException extends RuntimeException {
    public TransferFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}