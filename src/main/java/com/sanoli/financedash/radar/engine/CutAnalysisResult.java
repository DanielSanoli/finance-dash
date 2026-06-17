package com.sanoli.financedash.radar.engine;

import java.math.BigDecimal;
import java.util.List;

public record CutAnalysisResult(
        BigDecimal totalCuttable,
        BigDecimal currentProjectedBalance,
        BigDecimal projectedBalanceAfterCuts,
        List<CutAnalysisItem> items,
        List<String> assumptions
) {
}
