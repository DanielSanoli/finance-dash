package com.sanoli.financedash.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AppUserTest {

    @Test
    void shouldAllowTrialingUserWithActiveTrial() {
        AppUser user = user(SubscriptionStatus.TRIALING);
        user.setTrialEndsAt(LocalDateTime.now().plusDays(7));

        assertThat(user.hasActiveAccess()).isTrue();
        assertThat(user.getAccessMessage()).isEqualTo("Acesso ativo");
    }

    @Test
    void shouldBlockTrialingUserWithExpiredTrial() {
        AppUser user = user(SubscriptionStatus.TRIALING);
        user.setTrialEndsAt(LocalDateTime.now().minusDays(1));

        assertThat(user.hasActiveAccess()).isFalse();
        assertThat(user.getAccessMessage()).isEqualTo("Trial expirado");
    }

    @Test
    void shouldAllowActiveSubscriptionWithoutEndDate() {
        AppUser user = user(SubscriptionStatus.ACTIVE);

        assertThat(user.hasActiveAccess()).isTrue();
    }

    @Test
    void shouldBlockPastDueAndCanceledSubscriptions() {
        assertThat(user(SubscriptionStatus.PAST_DUE).hasActiveAccess()).isFalse();
        assertThat(user(SubscriptionStatus.CANCELED).hasActiveAccess()).isFalse();
    }

    private AppUser user(SubscriptionStatus status) {
        AppUser user = new AppUser();
        user.setName("Daniel");
        user.setEmail("daniel@example.com");
        user.setPasswordHash("hash");
        user.setSubscriptionStatus(status);
        user.setTrialEndsAt(LocalDateTime.now().plusDays(14));
        return user;
    }
}
