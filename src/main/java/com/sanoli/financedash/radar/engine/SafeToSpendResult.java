package com.sanoli.financedash.radar.engine;

import java.math.BigDecimal;
import java.util.List;

public record SafeToSpendResult(
        BigDecimal safeToSpendTotal,
        BigDecimal safeToSpendPerDay,
        int daysRemaining,
        BigDecimal reserveTarget,
        List<String> assumptions
) {
}
