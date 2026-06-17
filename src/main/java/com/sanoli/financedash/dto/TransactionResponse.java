package com.sanoli.financedash.dto;

import com.sanoli.financedash.domain.RecurrenceRule;
import com.sanoli.financedash.domain.Transaction;
import com.sanoli.financedash.domain.TransactionStatus;
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
        TransactionStatus status,
        LocalDate dueDate,
        boolean recurring,
        RecurrenceRule recurrenceRule,
        UUID clientId,
        String clientName,
        Boolean essential,
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
                transaction.getStatus(),
                transaction.getDueDate(),
                transaction.isRecurring(),
                transaction.getRecurrenceRule(),
                transaction.getClientId(),
                transaction.getClientName(),
                transaction.getEssential(),
                transaction.getPaymentMethod(),
                transaction.getNotes(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}
