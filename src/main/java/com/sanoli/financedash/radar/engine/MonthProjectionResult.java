package com.sanoli.financedash.radar.engine;

import java.math.BigDecimal;
import java.util.List;

public record MonthProjectionResult(
        BigDecimal currentBalance,
        BigDecimal projectedBalance,
        BigDecimal goal,
        boolean positive,
        List<String> assumptions
) {
}
