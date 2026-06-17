package com.sanoli.financedash.repository;

import com.sanoli.financedash.domain.Transaction;
import com.sanoli.financedash.domain.TransactionStatus;
import com.sanoli.financedash.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByUserIdAndTransactionDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByUserIdAndTypeAndStatusIn(UUID userId, TransactionType type, Collection<TransactionStatus> statuses);

    List<Transaction> findByUserIdAndStatusIn(UUID userId, Collection<TransactionStatus> statuses);
}
