package com.sanoli.financedash.dto;

import com.sanoli.financedash.domain.UserSettings;

import java.math.BigDecimal;
import java.util.UUID;

public record UserSettingsResponse(
        UUID id,
        UUID userId,
        BigDecimal monthlyIncomeGoal,
        BigDecimal monthlyReserveTarget,
        BigDecimal monthlyFixedCost,
        BigDecimal billableHoursPerMonth,
        BigDecimal taxRate,
        BigDecimal desiredMargin
) {
    public static UserSettingsResponse fromEntity(UserSettings settings) {
        return new UserSettingsResponse(
                settings.getId(),
                settings.getUserId(),
                settings.getMonthlyIncomeGoal(),
                settings.getMonthlyReserveTarget(),
                settings.getMonthlyFixedCost(),
                settings.getBillableHoursPerMonth(),
                settings.getTaxRate(),
                settings.getDesiredMargin()
        );
    }
}
