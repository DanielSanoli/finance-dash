package com.sanoli.financedash.dto;

import com.sanoli.financedash.domain.Transaction;
import com.sanoli.financedash.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String description,
        BigDecimal amount,
        TransactionType type,
        UUID categoryId,
        String categoryName,
        String categoryColor,
        LocalDate transactionDate,
        String paymentMethod,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TransactionResponse fromEntity(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName(),
                transaction.getCategory().getColor(),
                transaction.getTransactionDate(),
                transaction.getPaymentMethod(),
                transaction.getNotes(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}

