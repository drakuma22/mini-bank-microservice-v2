package com.minibank.transactionservice.service;

import com.minibank.transactionservice.client.AccountClient;
import com.minibank.transactionservice.client.AccountResponse;
import com.minibank.transactionservice.entity.Transaction;
import com.minibank.transactionservice.event.TransactionEvent;
import com.minibank.transactionservice.producer.TransactionProducer;
import com.minibank.transactionservice.repository.TransactionRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final TransactionProducer transactionProducer;
    private final RedisTemplate<String, String> redisTemplate;

    @Bulkhead(name = "transferOperation", type = Bulkhead.Type.THREADPOOL)
    public Transaction execute(String fromAccountNumber,
                               String toAccountNumber,
                               BigDecimal amount,
                               Transaction.TransactionType type) {

        String idempotencyKey = UUID.randomUUID().toString();

        Transaction transaction = new Transaction();
        transaction.setFromAccountNumber(fromAccountNumber);
        transaction.setToAccountNumber(toAccountNumber);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setCreatedAt(LocalDateTime.now());

        // 1. Verifica che il conto esista e abbia saldo sufficiente
        Optional<AccountResponse> accountOpt = accountClient.getAccount(fromAccountNumber);

        if (accountOpt.isEmpty()) {
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setFailureReason("account-service non disponibile o conto non trovato");
            return transactionRepository.save(transaction);
        }

        AccountResponse account = accountOpt.get();

        if (type == Transaction.TransactionType.WITHDRAWAL ||
                type == Transaction.TransactionType.TRANSFER) {
            if (account.getBalance().compareTo(amount) < 0) {
                transaction.setStatus(Transaction.TransactionStatus.FAILED);
                transaction.setFailureReason("Saldo insufficiente");
                return transactionRepository.save(transaction);
            }
        }

        // 2. Aggiorna il saldo
        BigDecimal newBalance;
        if (type == Transaction.TransactionType.DEPOSIT) {
            newBalance = account.getBalance().add(amount);
        } else {
            newBalance = account.getBalance().subtract(amount);
        }

        accountClient.updateBalance(fromAccountNumber, newBalance, idempotencyKey);

        // 3. Salva la transazione come completata
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        Transaction saved = transactionRepository.save(transaction);

        // Pubblica evento solo se la transazione è completata
        TransactionEvent event = new TransactionEvent(
                saved.getId(),
                saved.getFromAccountNumber(),
                saved.getToAccountNumber(),
                saved.getAmount(),
                saved.getType().name(),
                saved.getStatus().name(),
                saved.getCreatedAt()
        );
        transactionProducer.publish(event);

        return saved;
    }

    public List<Transaction> findByAccount(String accountNumber) {
        return transactionRepository.findByFromAccountNumber(accountNumber);
    }
}