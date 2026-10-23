package com.minibank.accountservice.controller;

import com.minibank.accountservice.dto.AccountResponse;
import com.minibank.accountservice.dto.CreateAccountRequest;
import com.minibank.accountservice.entity.Account;
import com.minibank.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URI;
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
    public List<AccountResponse> findAll() {
        return accountService.findAll().stream()
                .map(AccountResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> findById(@PathVariable Long id) {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            log.info("Richiesta gestita da: {}", hostname);
        } catch (Exception e) {
            log.warn("Impossibile determinare hostname");
        }
        return ResponseEntity.ok(AccountResponse.from(accountService.findById(id)));
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<AccountResponse> findByAccountNumber(@PathVariable String accountNumber) {
        log.info("Richiesta gestita dalla porta: {}", port);
        return ResponseEntity.ok(AccountResponse.from(accountService.findByAccountNumber(accountNumber)));
    }

    // Il client sceglie solo l'intestatario: numero, saldo e stato li decide il server
    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        Account created = accountService.open(request.ownerName());
        return ResponseEntity
                .created(URI.create("/accounts/number/" + created.getAccountNumber()))   // 201 + Location
                .body(AccountResponse.from(created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accountService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/number/{accountNumber}/debit")
    public ResponseEntity<AccountResponse> debit(
            @PathVariable String accountNumber,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody BigDecimal amount) {
        return ResponseEntity.ok(AccountResponse.from(accountService.debit(accountNumber, amount, idempotencyKey)));
    }

    @PostMapping("/number/{accountNumber}/credit")
    public ResponseEntity<AccountResponse> credit(
            @PathVariable String accountNumber,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody BigDecimal amount) {
        return ResponseEntity.ok(AccountResponse.from(accountService.credit(accountNumber, amount, idempotencyKey)));
    }
}