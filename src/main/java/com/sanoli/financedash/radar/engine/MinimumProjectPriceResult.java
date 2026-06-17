package com.sanoli.financedash.radar.engine;

import java.math.BigDecimal;
import java.util.List;

public record MinimumProjectPriceResult(
        BigDecimal estimatedHours,
        BigDecimal minimumProjectPrice,
        BigDecimal minimumHourlyRate,
        BigDecimal baseHourlyRate,
        BigDecimal taxRate,
        BigDecimal desiredMargin,
        List<String> assumptions
) {
}
