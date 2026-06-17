package com.sanoli.financedash.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record UserSettingsRequest(
        @PositiveOrZero(message = "monthlyIncomeGoal não pode ser negativo")
        BigDecimal monthlyIncomeGoal,

        @PositiveOrZero(message = "monthlyReserveTarget não pode ser negativo")
        BigDecimal monthlyReserveTarget,

        @PositiveOrZero(message = "monthlyFixedCost não pode ser negativo")
        BigDecimal monthlyFixedCost,

        @PositiveOrZero(message = "billableHoursPerMonth não pode ser negativo")
        BigDecimal billableHoursPerMonth,

        @DecimalMin(value = "0.0", message = "taxRate deve estar entre 0 e 1")
        @DecimalMax(value = "1.0", inclusive = false, message = "taxRate deve estar entre 0 e 1")
        BigDecimal taxRate,

        @DecimalMin(value = "0.0", message = "desiredMargin deve estar entre 0 e 1")
        @DecimalMax(value = "1.0", inclusive = false, message = "desiredMargin deve estar entre 0 e 1")
        BigDecimal desiredMargin
) {
}
