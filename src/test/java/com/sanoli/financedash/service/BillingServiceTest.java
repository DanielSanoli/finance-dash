package com.sanoli.financedash.service;

import com.sanoli.financedash.billing.AsaasClient;
import com.sanoli.financedash.config.AsaasProperties;
import com.sanoli.financedash.domain.AppUser;
import com.sanoli.financedash.domain.SubscriptionPlan;
import com.sanoli.financedash.domain.SubscriptionStatus;
import com.sanoli.financedash.dto.CheckoutResponse;
import com.sanoli.financedash.repository.UserRepository;
import com.sanoli.financedash.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AsaasClient asaasClient;

    private AsaasProperties asaasProperties;
    private BillingService billingService;

    @BeforeEach
    void setUp() {
        asaasProperties = new AsaasProperties();
        asaasProperties.setProPlanValue(new BigDecimal("29.90"));
        billingService = new BillingService(currentUserService, userRepository, asaasClient, asaasProperties);
    }

    @Test
    void shouldReturnGuidanceWhenAsaasIsDisabled() {
        AppUser user = user();
        when(currentUserService.getCurrentUser()).thenReturn(user);

        CheckoutResponse response = billingService.createProCheckout();

        assertThat(response.checkoutUrl()).isNull();
        assertThat(response.message()).contains("ASAAS_ENABLED");
    }

    private AppUser user() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setName("Daniel");
        user.setEmail("daniel@example.com");
        user.setPasswordHash("hash");
        user.setPlan(SubscriptionPlan.FREE);
        user.setSubscriptionStatus(SubscriptionStatus.TRIALING);
        return user;
    }
}
