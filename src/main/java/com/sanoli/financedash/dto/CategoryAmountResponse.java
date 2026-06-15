package com.sanoli.financedash.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryAmountResponse(
        UUID categoryId,
        String categoryName,
        String categoryColor,
        BigDecimal amount,
        BigDecimal percentage
) {
}

