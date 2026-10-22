package com.minibank.accountservice.exception.custom;

public class AccountNotFoundException extends RuntimeException{
    public AccountNotFoundException(String accountNumber){
        super("Conto non trovato:" + accountNumber);
    }
}
