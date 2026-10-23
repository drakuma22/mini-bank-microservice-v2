package com.minibank.transactionservice.dto;

import com.minibank.transactionservice.enums.TransactionType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest(

        @NotBlank(message = "Il conto di origine è obbligatorio")
        String fromAccountNumber,

        String toAccountNumber,   // obbligatorio solo per TRANSFER: vedi la regola sotto

        @NotNull(message = "L'importo è obbligatorio")
        @Positive(message = "L'importo deve essere maggiore di zero")
        @Digits(integer = 15, fraction = 2, message = "L'importo può avere al massimo 2 decimali")
        BigDecimal amount,

        @NotNull(message = "Il tipo di operazione è obbligatorio")
        TransactionType type
) {
    // Regola che coinvolge due campi insieme: si scrive come metodo booleano
    @AssertTrue(message = "Per un TRANSFER il conto destinatario è obbligatorio e diverso dal conto di origine")
    public boolean isDestinationValid() {
        if (type != TransactionType.TRANSFER) return true;
        return toAccountNumber != null
                && !toAccountNumber.isBlank()
                && !toAccountNumber.equals(fromAccountNumber);
    }
}