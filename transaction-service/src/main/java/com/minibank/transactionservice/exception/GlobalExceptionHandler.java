package com.minibank.transactionservice.exception;

import com.minibank.transactionservice.exception.custom.TransferFailedException;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BulkheadFullException.class)
    public ProblemDetail handleBulkheadFull(BulkheadFullException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Servizio temporaneamente sovraccarico, riprova tra qualche istante");
        problem.setProperty("code", "TRANSFER_SERVICE_BUSY");   // il tuo codice resta, come campo extra
        return problem;
    }
}