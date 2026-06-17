package com.sanoli.financedash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sanoli.financedash.billing.AsaasClient;
import com.sanoli.financedash.config.AsaasProperties;
import com.sanoli.financedash.domain.AppUser;
import com.sanoli.financedash.domain.SubscriptionPlan;
import com.sanoli.financedash.domain.SubscriptionStatus;
import com.sanoli.financedash.dto.CheckoutResponse;
import com.sanoli.financedash.exception.WebhookUnauthorizedException;
import com.sanoli.financedash.repository.UserRepository;
import com.sanoli.financedash.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    private static final String SUBSCRIPTION_ID = "sub_123";
    private static final String CUSTOMER_ID = "cus_123";
    private static final String WEBHOOK_TOKEN = "test-webhook-token";

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AsaasClient asaasClient;

    private AsaasProperties asaasProperties;
    private BillingService billingService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        asaasProperties = new AsaasProperties();
        asaasProperties.setProPlanValue(new BigDecimal("29.90"));
        asaasProperties.setEnabled(true);
        asaasProperties.setApiKey("test-key");
        asaasProperties.setWebhookToken(WEBHOOK_TOKEN);
        billingService = new BillingService(currentUserService, userRepository, asaasClient, asaasProperties);
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldReturnGuidanceWhenAsaasIsDisabled() {
        asaasProperties.setEnabled(false);
        AppUser user = trialingUser();
        when(currentUserService.getCurrentUser()).thenReturn(user);

        CheckoutResponse response = billingService.createProCheckout();

        assertThat(response.checkoutUrl()).isNull();
        assertThat(response.message()).contains("ASAAS_ENABLED");
        assertThat(user.hasActiveAccess()).isTrue();
    }

    @Test
    void shouldKeepTrialAccessAfterStartingCheckout() {
        AppUser user = trialingUser();
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(asaasClient.createCustomer(user.getName(), user.getEmail())).thenReturn(CUSTOMER_ID);
        when(asaasClient.createSubscription(CUSTOMER_ID, asaasProperties.getProPlanValue()))
                .thenReturn(new AsaasClient.AsaasSubscriptionResult(SUBSCRIPTION_ID, "https://pay.asaas.com/invoice/1"));

        CheckoutResponse response = billingService.createProCheckout();

        assertThat(response.checkoutUrl()).isEqualTo("https://pay.asaas.com/invoice/1");
        assertThat(user.getPlan()).isEqualTo(SubscriptionPlan.FREE);
        assertThat(user.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.TRIALING);
        assertThat(user.getAsaasSubscriptionId()).isEqualTo(SUBSCRIPTION_ID);
        assertThat(user.hasActiveAccess()).isTrue();

        ArgumentCaptor<AppUser> savedUser = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getAsaasSubscriptionId()).isEqualTo(SUBSCRIPTION_ID);
    }

    @Test
    void shouldActivateUserWhenPaymentIsConfirmed() {
        AppUser user = trialingUser();
        user.setAsaasSubscriptionId(SUBSCRIPTION_ID);
        when(userRepository.findByAsaasSubscriptionId(SUBSCRIPTION_ID)).thenReturn(Optional.of(user));

        billingService.handleWebhook(WEBHOOK_TOKEN, webhookPayload("PAYMENT_CONFIRMED", SUBSCRIPTION_ID));

        assertThat(user.getPlan()).isEqualTo(SubscriptionPlan.PRO);
        assertThat(user.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(user.getSubscriptionEndsAt()).isAfter(LocalDateTime.now());
        verify(userRepository).save(user);
    }

    @Test
    void shouldCancelUserWhenSubscriptionIsDeleted() {
        AppUser user = trialingUser();
        user.setAsaasSubscriptionId(SUBSCRIPTION_ID);
        user.setPlan(SubscriptionPlan.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        when(userRepository.findByAsaasSubscriptionId(SUBSCRIPTION_ID)).thenReturn(Optional.of(user));

        billingService.handleWebhook(WEBHOOK_TOKEN, webhookPayload("SUBSCRIPTION_DELETED", SUBSCRIPTION_ID));

        assertThat(user.getPlan()).isEqualTo(SubscriptionPlan.FREE);
        assertThat(user.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        verify(userRepository).save(user);
    }

    @Test
    void shouldNotSaveUserWhenWebhookEventIsIdempotent() {
        AppUser user = trialingUser();
        user.setAsaasSubscriptionId(SUBSCRIPTION_ID);
        user.setPlan(SubscriptionPlan.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        user.setSubscriptionEndsAt(LocalDateTime.now().plusMonths(1));
        when(userRepository.findByAsaasSubscriptionId(SUBSCRIPTION_ID)).thenReturn(Optional.of(user));

        billingService.handleWebhook(WEBHOOK_TOKEN, webhookPayload("PAYMENT_CONFIRMED", SUBSCRIPTION_ID));

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRejectWebhookWhenTokenIsMissingOrInvalid() {
        assertThatThrownBy(() -> billingService.handleWebhook(null, webhookPayload("PAYMENT_CONFIRMED", SUBSCRIPTION_ID)))
                .isInstanceOf(WebhookUnauthorizedException.class)
                .hasMessageContaining("inválido");

        assertThatThrownBy(() -> billingService.handleWebhook("wrong-token", webhookPayload("PAYMENT_CONFIRMED", SUBSCRIPTION_ID)))
                .isInstanceOf(WebhookUnauthorizedException.class)
                .hasMessageContaining("inválido");

        verify(userRepository, never()).findByAsaasSubscriptionId(any());
    }

    @Test
    void shouldIgnoreWebhookWhenBillingIsDisabled() {
        asaasProperties.setEnabled(false);

        billingService.handleWebhook(null, webhookPayload("PAYMENT_CONFIRMED", SUBSCRIPTION_ID));

        verify(userRepository, never()).findByAsaasSubscriptionId(any());
    }

    private ObjectNode webhookPayload(String event, String subscriptionId) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("event", event);
        ObjectNode payment = payload.putObject("payment");
        payment.put("subscription", subscriptionId);
        return payload;
    }

    private AppUser trialingUser() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setName("Daniel");
        user.setEmail("daniel@example.com");
        user.setPasswordHash("hash");
        user.setPlan(SubscriptionPlan.FREE);
        user.setSubscriptionStatus(SubscriptionStatus.TRIALING);
        user.setTrialEndsAt(LocalDateTime.now().plusDays(10));
        return user;
    }
}
