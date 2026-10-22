package com.minibank.accountservice.service;

import com.minibank.accountservice.exception.custom.AccountNotFoundException;
import com.minibank.accountservice.exception.custom.InsufficientFundsException;
import com.minibank.accountservice.exception.custom.OperationInProgressException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.minibank.accountservice.entity.Account;
import com.minibank.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    public Account findById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("id: " + id));
    }

    @Cacheable(value = "accounts", key = "#accountNumber")
    public Account findByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    public Account save(Account account) {
        return accountRepository.save(account);
    }

    @CacheEvict(value = "accounts", allEntries = true)
    public void deleteById(Long id) {
        accountRepository.deleteById(id);
    }

    @CacheEvict(value = "accounts", key = "#accountNumber")
    public Account debit(String accountNumber, BigDecimal amount, String idempotencyKey) {
        return executeIdempotent(idempotencyKey, () -> {
            int updated = accountRepository.debit(accountNumber, amount);
            if (updated == 0) {
                // 0 righe: o il conto non esiste, o il saldo non basta. Distinguo i due casi.
                if (accountRepository.findByAccountNumber(accountNumber).isEmpty()) {
                    throw new AccountNotFoundException(accountNumber);
                }
                throw new InsufficientFundsException(accountNumber, amount);
            }
            // rilettura DOPO la modifica: serve solo a restituire il saldo aggiornato
            return accountRepository.findByAccountNumber(accountNumber).orElseThrow();
        });
    }

    @CacheEvict(value = "accounts", key = "#accountNumber")
    public Account credit(String accountNumber, BigDecimal amount, String idempotencyKey) {
        return executeIdempotent(idempotencyKey, () -> {
            int updated = accountRepository.credit(accountNumber, amount);
            if (updated == 0) {
                throw new AccountNotFoundException(accountNumber);
            }
            return accountRepository.findByAccountNumber(accountNumber).orElseThrow();
        });
    }

    // La logica di idempotenza, scritta una volta sola
    private Account executeIdempotent(String idempotencyKey, Supplier<Account> operation) {
        String redisKey = "idempotency:" + idempotencyKey;

        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSING", Duration.ofHours(24));

        if (Boolean.FALSE.equals(isNew)) {
            String existingResult = redisTemplate.opsForValue().get(redisKey);
            if ("PROCESSING".equals(existingResult)) {
                throw new OperationInProgressException("Operazione già in corso per la key " + idempotencyKey);
            }
            log.info("IDEMPOTENT HIT — key {} già processata, nessun update eseguito", idempotencyKey);
            try {
                return objectMapper.readValue(existingResult, Account.class);
            } catch (JacksonException e) {
                throw new RuntimeException("Errore deserializzazione", e);
            }
        }

        log.info("NUOVA OPERAZIONE — key {} mai vista, eseguo", idempotencyKey);

        Account result;
        try {
            result = operation.get();          // ← qui viene eseguito il debit o il credit
        } catch (RuntimeException e) {
            redisTemplate.delete(redisKey);    // fallita: libero la chiave
            throw e;
        }

        try {
            redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(result), Duration.ofHours(24));
        } catch (JacksonException e) {
            log.error("Errore serializzazione idempotency key {}", idempotencyKey, e);
        }
        return result;
    }
}