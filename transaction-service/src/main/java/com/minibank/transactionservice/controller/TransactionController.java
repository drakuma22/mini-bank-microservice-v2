package com.minibank.transactionservice.controller;

import com.minibank.transactionservice.dto.TransactionResponse;
import com.minibank.transactionservice.dto.TransferRequest;
import com.minibank.transactionservice.entity.Transaction;
import com.minibank.transactionservice.enums.TransactionStatus;
import com.minibank.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> execute(@Valid @RequestBody TransferRequest request) {
        Transaction result = transactionService.execute(
                request.fromAccountNumber(), request.toAccountNumber(), request.amount(), request.type());

        TransactionResponse body = TransactionResponse.from(result);

        // la chiamata è andata; l'operazione può essere fallita → 422 con il motivo nel corpo
        return result.getStatus() == TransactionStatus.FAILED
                ? ResponseEntity.unprocessableEntity().body(body)
                : ResponseEntity.ok(body);
    }

    @GetMapping("/account/{accountNumber}")
    public List<TransactionResponse> findByAccount(@PathVariable String accountNumber) {
        return transactionService.findByAccount(accountNumber).stream()
                .map(TransactionResponse::from)
                .toList();
    }
}