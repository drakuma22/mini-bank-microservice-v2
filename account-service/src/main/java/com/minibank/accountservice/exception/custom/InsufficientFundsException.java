package com.minibank.accountservice.exception.custom;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException{
    public InsufficientFundsException(String accountNumber, BigDecimal amount){
        super("Fondi insufficienti sul conto " + accountNumber + ": impossibile prelevare " + amount);
    }
}
