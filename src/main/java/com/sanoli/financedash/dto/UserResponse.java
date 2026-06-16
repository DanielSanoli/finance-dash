package com.sanoli.financedash.dto;

import com.sanoli.financedash.domain.AppUser;
import com.sanoli.financedash.domain.SubscriptionPlan;
import com.sanoli.financedash.domain.SubscriptionStatus;
import com.sanoli.financedash.domain.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        SubscriptionPlan plan,
        SubscriptionStatus subscriptionStatus,
        LocalDateTime trialEndsAt,
        LocalDateTime subscriptionEndsAt
) {
    public static UserResponse fromEntity(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPlan(),
                user.getSubscriptionStatus(),
                user.getTrialEndsAt(),
                user.getSubscriptionEndsAt()
        );
    }
}
