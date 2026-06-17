package com.sanoli.financedash.radar.engine;

import com.sanoli.financedash.domain.TransactionStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record CutAnalysisItem(
        UUID transactionId,
        String description,
        String categoryName,
        BigDecimal amount,
        TransactionStatus status
) {
}
