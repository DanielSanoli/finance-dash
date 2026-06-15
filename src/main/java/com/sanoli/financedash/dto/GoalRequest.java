package com.sanoli.financedash.dto;

import com.sanoli.financedash.domain.GoalType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record GoalRequest(
        @NotBlank(message = "title é obrigatório")
        String title,

        @NotNull(message = "month é obrigatório")
        @Min(value = 1, message = "month deve estar entre 1 e 12")
        @Max(value = 12, message = "month deve estar entre 1 e 12")
        Integer month,

        @NotNull(message = "year é obrigatório")
        @Min(value = 1900, message = "year deve ser maior ou igual a 1900")
        Integer year,

        @NotNull(message = "targetAmount é obrigatório")
        @DecimalMin(value = "0.0", inclusive = false, message = "targetAmount deve ser maior que zero")
        BigDecimal targetAmount,

        @NotNull(message = "type é obrigatório")
        GoalType type,

        UUID categoryId
) {
}

