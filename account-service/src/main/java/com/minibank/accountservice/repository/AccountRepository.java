package com.minibank.accountservice.repository;

import com.minibank.accountservice.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Account a SET a.balance = a.balance - :amount " +
            "WHERE a.accountNumber = :accountNumber AND a.balance >= :amount")
    int debit(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);

    // Accredito atomico: nessun controllo sul saldo, un accredito non può fallire per fondi
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Account a SET a.balance = a.balance + :amount " +
            "WHERE a.accountNumber = :accountNumber")
    int credit(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);
}