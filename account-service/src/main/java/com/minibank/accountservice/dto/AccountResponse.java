package com.minibank.accountservice.dto;

import com.minibank.accountservice.entity.Account;
import com.minibank.accountservice.enums.AccountStatus;

import java.math.BigDecimal;

public record AccountResponse(
        Long id,
        String accountNumber,
        String ownerName,
        BigDecimal balance,
        AccountStatus status
) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getAccountNumber(), a.getOwnerName(), a.getBalance(), a.getStatus());
    }
}
