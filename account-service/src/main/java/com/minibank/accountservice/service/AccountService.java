package com.minibank.accountservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public Account findByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));
    }

    public Account save(Account account) {
        return accountRepository.save(account);
    }

    public void deleteById(Long id) {
        accountRepository.deleteById(id);
    }

    public Account updateBalanceIdempotence(String accountNumber, BigDecimal newBalance, String idempotencyKey) {

        String redisKey = "idempotency:" + idempotencyKey;

        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSING", Duration.ofHours(24));

        if(isNew != null){
            log.info("IDEMPOTENT HIT — key {} già processata, nessun update eseguito", idempotencyKey);
            String existingResult = redisTemplate.opsForValue().get(redisKey);
            try{
                return objectMapper.readValue(existingResult, Account.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Errore deserializzazione", e);
            }
        }

        log.info("NUOVA OPERAZIONE — key {} mai vista, eseguo update sul DB", idempotencyKey);

        Account account = findByAccountNumber(accountNumber);
        account.setBalance(newBalance);
        Account saved = save(account);

        try {
            redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(saved), Duration.ofHours(24));
            log.info("Risultato salvato in Redis per key {}", idempotencyKey);
        } catch (JsonProcessingException e) {
            log.error("Errore serializzazione idempotency key {}", idempotencyKey, e);
        }

        return saved;
    }
}