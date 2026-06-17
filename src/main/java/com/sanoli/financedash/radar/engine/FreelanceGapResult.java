package com.sanoli.financedash.radar.engine;

import java.math.BigDecimal;
import java.util.List;

public record FreelanceGapResult(
        BigDecimal monthlyIncomeGoal,
        BigDecimal expectedMonthIncome,
        BigDecimal incomeGap,
        BigDecimal referenceHourlyRate,
        BigDecimal extraBillableHours,
        boolean needsMoreFreelance,
        List<String> assumptions
) {
}
