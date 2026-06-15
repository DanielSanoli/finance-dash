package com.sanoli.financedash.dto;

import com.sanoli.financedash.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionRequest(
        @NotBlank(message = "description é obrigatório")
        String description,

        @NotNull(message = "amount é obrigatório")
        @DecimalMin(value = "0.0", inclusive = false, message = "amount deve ser maior que zero")
        BigDecimal amount,

        @NotNull(message = "type é obrigatório")
        TransactionType type,

        @NotNull(message = "categoryId é obrigatório")
        UUID categoryId,

        @NotNull(message = "transactionDate é obrigatório")
        LocalDate transactionDate,

        String paymentMethod,
        String notes
) {
}

