package com.sanoli.financedash.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MonthlyDashboardResponse(
        Integer month,
        Integer year,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        long transactionCount,
        long incomeCount,
        long expenseCount,
        List<CategoryAmountResponse> expensesByCategory,
        List<CategoryAmountResponse> incomesByCategory
) {
}

