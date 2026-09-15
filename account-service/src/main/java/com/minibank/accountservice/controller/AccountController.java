package com.minibank.accountservice.controller;

import com.minibank.accountservice.entity.Account;
import com.minibank.accountservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    @Value("${server.port}")
    private String port;

    private final AccountService accountService;

    @GetMapping
    public List<Account> findAll() {
        return accountService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> findById(@PathVariable Long id) {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            log.info("Richiesta gestita da: {}", hostname);
        } catch (Exception e) {
            log.warn("Impossibile determinare hostname");
        }

        return ResponseEntity.ok(accountService.findById(id));
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<Account> findByAccountNumber(@PathVariable String accountNumber) {
        log.info("Richiesta gestita dalla porta: {}", port);
        return ResponseEntity.ok(accountService.findByAccountNumber(accountNumber));
    }

    @PostMapping
    public ResponseEntity<Account> create(@RequestBody Account account) {
        return ResponseEntity.ok(accountService.save(account));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accountService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/number/{accountNumber}/balance")
    public ResponseEntity<Account> updateBalance(
            @PathVariable String accountNumber,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody BigDecimal newBalance) {
        Account account = accountService.updateBalanceIdempotence(accountNumber, newBalance, idempotencyKey);
        return ResponseEntity.ok(account);
    }
}