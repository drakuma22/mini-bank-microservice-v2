package com.minibank.transactionservice.controller;

import com.minibank.transactionservice.entity.Transaction;
import com.minibank.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<Transaction> execute(
            @RequestParam String fromAccountNumber,
            @RequestParam(required = false) String toAccountNumber,
            @RequestParam BigDecimal amount,
            @RequestParam Transaction.TransactionType type) {

        Transaction result = transactionService.execute(
                fromAccountNumber, toAccountNumber, amount, type);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/account/{accountNumber}")
    public List<Transaction> findByAccount(@PathVariable String accountNumber) {
        return transactionService.findByAccount(accountNumber);
    }
}