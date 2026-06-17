package com.sanoli.financedash.dto;

import com.sanoli.financedash.domain.SubscriptionPlan;

public record CheckoutResponse(
        SubscriptionPlan plan,
        String checkoutUrl,
        String message
) {
}
