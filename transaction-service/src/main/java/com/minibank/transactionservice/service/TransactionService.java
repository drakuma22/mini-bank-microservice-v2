package com.minibank.transactionservice.service;

import com.minibank.transactionservice.client.AccountClient;
import com.minibank.transactionservice.entity.Transaction;
import com.minibank.transactionservice.event.TransactionEvent;
import com.minibank.transactionservice.exception.custom.TransferFailedException;
import com.minibank.transactionservice.producer.TransactionProducer;
import com.minibank.transactionservice.repository.TransactionRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final TransactionProducer transactionProducer;

    @Bulkhead(name = "transferOperation")
    public Transaction execute(String fromAccountNumber,
                               String toAccountNumber,
                               BigDecimal amount,
                               Transaction.TransactionType type) {

        // Un id per tutta l'operazione; ogni passo ne deriva una chiave di idempotenza propria
        String operationId = UUID.randomUUID().toString();

        Transaction transaction = new Transaction();
        transaction.setFromAccountNumber(fromAccountNumber);
        transaction.setToAccountNumber(toAccountNumber);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setCreatedAt(LocalDateTime.now());

        try {
            switch (type) {
                case DEPOSIT    -> accountClient.credit(fromAccountNumber, amount, operationId + ":credit");
                case WITHDRAWAL -> accountClient.debit(fromAccountNumber, amount, operationId + ":debit");
                case TRANSFER   -> transfer(fromAccountNumber, toAccountNumber, amount, operationId);
            }
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);

        } catch (TransferFailedException e) {
            // trasferimento fallito dopo l'addebito: il messaggio dice com'è finita la compensazione
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());

        } catch (RuntimeException e) {
            // fallito il primo passo: nessun soldo si è mosso
            log.warn("Operazione {} fallita: {}", operationId, e.getMessage());
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setFailureReason(reasonFor(e));
        }

        Transaction saved = transactionRepository.save(transaction);

        if (saved.getStatus() == Transaction.TransactionStatus.COMPLETED) {
            transactionProducer.publish(new TransactionEvent(
                    saved.getId(),
                    saved.getFromAccountNumber(),
                    saved.getToAccountNumber(),
                    saved.getAmount(),
                    saved.getType().name(),
                    saved.getStatus().name(),
                    saved.getCreatedAt()
            ));
        }
        return saved;
    }

    // Saga: addebito → accredito; se l'accredito fallisce, compensazione (riaccredito al mittente)
    private void transfer(String from, String to, BigDecimal amount, String operationId) {
        accountClient.debit(from, amount, operationId + ":debit");   // se fallisce qui, nulla da annullare

        try {
            accountClient.credit(to, amount, operationId + ":credit");
        } catch (RuntimeException creditError) {
            log.warn("Operazione {}: accredito su {} fallito, avvio compensazione", operationId, to);
            try {
                accountClient.credit(from, amount, operationId + ":compensation");
            } catch (RuntimeException compensationError) {
                // il caso peggiore: soldi addebitati e non restituiti. Serve un intervento umano.
                log.error("COMPENSAZIONE FALLITA — operazione {}: {} addebitati su {} e NON restituiti",
                        operationId, amount, from, compensationError);
                throw new TransferFailedException(
                        "Compensazione fallita: intervento manuale richiesto (operazione " + operationId + ")",
                        compensationError);
            }
            throw new TransferFailedException(
                    reasonFor(creditError) + " (destinatario " + to + "): addebito stornato", creditError);
        }
    }

    // Traduce l'errore di account-service in un motivo leggibile
    private String reasonFor(RuntimeException e) {
        if (e instanceof HttpClientErrorException.UnprocessableEntity) return "Saldo insufficiente";
        if (e instanceof HttpClientErrorException.NotFound) return "Conto non trovato";
        return "account-service non disponibile";
    }

    public List<Transaction> findByAccount(String accountNumber) {
        return transactionRepository.findByFromAccountNumber(accountNumber);
    }
}