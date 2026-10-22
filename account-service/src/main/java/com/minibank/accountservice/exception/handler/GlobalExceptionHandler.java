package com.minibank.accountservice.exception.handler;

import com.minibank.accountservice.exception.custom.AccountNotFoundException;
import com.minibank.accountservice.exception.custom.InsufficientFundsException;
import com.minibank.accountservice.exception.custom.OperationInProgressException ;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleNotFound(AccountNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ProblemDetail handleInsufficientFunds(InsufficientFundsException e) {
        log.warn("Addebito rifiutato: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    // la stessa chiave di idempotenza è ancora in elaborazione
    @ExceptionHandler(OperationInProgressException .class)
    public ProblemDetail handleConflict(OperationInProgressException  e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
