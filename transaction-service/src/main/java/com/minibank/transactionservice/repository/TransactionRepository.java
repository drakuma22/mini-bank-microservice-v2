package com.minibank.transactionservice.repository;

import com.minibank.transactionservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByFromAccountNumber(String accountNumber);
}