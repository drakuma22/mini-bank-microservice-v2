package com.minibank.accountservice.service;

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
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));
    }

    @Cacheable(value = "accounts", key = "#accountNumber")
    public Account findByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));
    }

    public Account save(Account account) {
        return accountRepository.save(account);
    }

    @CacheEvict(value = "accounts", allEntries = true)
    public void deleteById(Long id) {
        accountRepository.deleteById(id);
    }

    @CacheEvict(value = "accounts", key = "#accountNumber")
    public Account updateBalanceIdempotence(String accountNumber, BigDecimal newBalance, String idempotencyKey) {

        String redisKey = "idempotency:" + idempotencyKey;

        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSING", Duration.ofHours(24));

        // La chiave esisteva già: operazione ripetuta
        if (Boolean.FALSE.equals(isNew)) {
            String existingResult = redisTemplate.opsForValue().get(redisKey);

            // (1) La prima richiesta è ancora in corso: non c'è ancora un risultato da restituire
            if ("PROCESSING".equals(existingResult)) {
                throw new IllegalStateException("Operazione già in corso per la key " + idempotencyKey);
            }

            log.info("IDEMPOTENT HIT — key {} già processata, nessun update eseguito", idempotencyKey);
            try {
                return objectMapper.readValue(existingResult, Account.class);
            } catch (JacksonException e) {
                throw new RuntimeException("Errore deserializzazione", e);
            }
        }

        log.info("NUOVA OPERAZIONE — key {} mai vista, eseguo update sul DB", idempotencyKey);

        Account saved;
        try {
            Account account = findByAccountNumber(accountNumber);
            account.setBalance(newBalance);
            saved = save(account);
        } catch (RuntimeException e) {
            // (2) L'aggiornamento è fallito: libero la chiave, così un nuovo tentativo può riprovare
            redisTemplate.delete(redisKey);
            throw e;
        }

        try {
            redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(saved), Duration.ofHours(24));
            log.info("Risultato salvato in Redis per key {}", idempotencyKey);
        } catch (JacksonException e) {
            log.error("Errore serializzazione idempotency key {}", idempotencyKey, e);
        }

        return saved;
    }
}