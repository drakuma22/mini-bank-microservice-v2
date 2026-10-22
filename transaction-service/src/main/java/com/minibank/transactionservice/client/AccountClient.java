package com.minibank.transactionservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@Slf4j
public class AccountClient {

    private final RestClient restClient;

    public AccountClient(RestClient accountRestClient) {
        this.restClient = accountRestClient;
    }

    @CircuitBreaker(name = "accountService")
    @Retry(name = "accountService", fallbackMethod = "getAccountFallback")
    public Optional<AccountResponse> getAccount(String accountNumber) {
        log.info("Calling account-service for account: {}", accountNumber);
        AccountResponse response = restClient.get()
                .uri("/accounts/number/{accountNumber}", accountNumber)
                .retrieve()
                .body(AccountResponse.class);
        return Optional.ofNullable(response);
    }

    @CircuitBreaker(name = "accountService")
    @Retry(name = "accountService")
    public AccountResponse debit(String accountNumber, BigDecimal amount, String idempotencyKey) {
        log.info("Debit {} on account {}", amount, accountNumber);
        return restClient.post()
                .uri("/accounts/number/{accountNumber}/debit", accountNumber)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(amount)
                .retrieve()
                .body(AccountResponse.class);
    }

    @CircuitBreaker(name = "accountService")
    @Retry(name = "accountService")
    public AccountResponse credit(String accountNumber, BigDecimal amount, String idempotencyKey) {
        log.info("Credit {} on account {}", amount, accountNumber);
        return restClient.post()
                .uri("/accounts/number/{accountNumber}/credit", accountNumber)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(amount)
                .retrieve()
                .body(AccountResponse.class);
    }

    public Optional<AccountResponse> getAccountFallback(String accountNumber, Exception e) {
        log.error("Fallback for getAccount - account: {}, error: {}", accountNumber, e.getMessage());
        return Optional.empty();
    }
}