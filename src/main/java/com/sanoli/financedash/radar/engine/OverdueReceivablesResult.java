package com.sanoli.financedash.radar.engine;

import java.math.BigDecimal;
import java.util.List;

public record OverdueReceivablesResult(
        BigDecimal totalBlocked,
        List<OverdueReceivableItem> items,
        List<String> assumptions
) {
}
