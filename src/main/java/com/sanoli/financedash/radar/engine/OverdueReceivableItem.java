package com.sanoli.financedash.radar.engine;

import java.math.BigDecimal;
import java.util.UUID;

public record OverdueReceivableItem(
        UUID clientId,
        String clientName,
        BigDecimal amount,
        long daysOverdue,
        BigDecimal impact
) {
}
