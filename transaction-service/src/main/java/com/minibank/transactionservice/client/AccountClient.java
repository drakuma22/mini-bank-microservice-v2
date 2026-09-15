package com.minibank.transactionservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
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
    @Retry(name = "accountService", fallbackMethod = "updateBalanceFallback")
    public AccountResponse updateBalance(String accountNumber, BigDecimal newBalance, String idempotency) {
        log.info("Updating balance for account: {}", accountNumber);
        return restClient.patch()
                .uri("/accounts/number/{accountNumber}/balance", accountNumber)
                .header("Idempotency-Key", idempotency)
                .body(newBalance)
                .retrieve()
                .body(AccountResponse.class);
    }

    public Optional<AccountResponse> getAccountFallback(String accountNumber, Exception e) {
        log.error("Fallback for getAccount - account: {}, error: {}", accountNumber, e.getMessage());
        return Optional.empty();
    }

    public AccountResponse updateBalanceFallback(String accountNumber, BigDecimal newBalance, Exception e) {
        log.error("Fallback for updateBalance - account: {}, error: {}", accountNumber, e.getMessage());
        throw new RuntimeException("account-service non disponibile, impossibile aggiornare il saldo");
    }
}