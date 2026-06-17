package com.sanoli.financedash.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sanoli.financedash.billing.AsaasClient;
import com.sanoli.financedash.config.AsaasProperties;
import com.sanoli.financedash.domain.AppUser;
import com.sanoli.financedash.domain.SubscriptionPlan;
import com.sanoli.financedash.domain.SubscriptionStatus;
import com.sanoli.financedash.dto.CheckoutResponse;
import com.sanoli.financedash.exception.BusinessException;
import com.sanoli.financedash.exception.WebhookUnauthorizedException;
import com.sanoli.financedash.repository.UserRepository;
import com.sanoli.financedash.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class BillingService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final AsaasClient asaasClient;
    private final AsaasProperties asaasProperties;

    public BillingService(
            CurrentUserService currentUserService,
            UserRepository userRepository,
            AsaasClient asaasClient,
            AsaasProperties asaasProperties
    ) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.asaasClient = asaasClient;
        this.asaasProperties = asaasProperties;
    }

    @Transactional
    public CheckoutResponse createProCheckout() {
        AppUser user = currentUserService.getCurrentUser();
        if (user.getPlan() == SubscriptionPlan.PRO && user.getSubscriptionStatus() == SubscriptionStatus.ACTIVE) {
            throw new BusinessException("Plano Pro já está ativo");
        }

        if (!asaasProperties.isEnabled()) {
            return new CheckoutResponse(
                    SubscriptionPlan.PRO,
                    null,
                    "Configure ASAAS_ENABLED=true e ASAAS_API_KEY para habilitar checkout real."
            );
        }

        if (user.getAsaasCustomerId() == null) {
            user.setAsaasCustomerId(asaasClient.createCustomer(user.getName(), user.getEmail()));
        }

        AsaasClient.AsaasSubscriptionResult subscription = asaasClient.createSubscription(
                user.getAsaasCustomerId(),
                asaasProperties.getProPlanValue()
        );
        user.setAsaasSubscriptionId(subscription.subscriptionId());
        userRepository.save(user);

        return new CheckoutResponse(
                SubscriptionPlan.PRO,
                subscription.checkoutUrl(),
                "Assinatura criada. Conclua o pagamento para ativar o plano Pro."
        );
    }

    @Transactional
    public void handleWebhook(String accessToken, JsonNode payload) {
        if (!asaasProperties.isEnabled()) {
            return;
        }

        validateWebhookToken(accessToken);

        String event = payload.path("event").asText("");
        JsonNode payment = payload.path("payment");
        String subscriptionId = payment.path("subscription").asText(null);
        if (subscriptionId == null || subscriptionId.isBlank()) {
            return;
        }

        AppUser user = userRepository.findByAsaasSubscriptionId(subscriptionId)
                .orElseThrow(() -> new BusinessException("Assinatura não encontrada"));

        boolean changed = switch (event) {
            case "PAYMENT_CONFIRMED", "PAYMENT_RECEIVED" -> activateSubscription(user);
            case "PAYMENT_OVERDUE" -> applyPastDue(user);
            case "PAYMENT_DELETED", "SUBSCRIPTION_DELETED" -> cancelSubscription(user);
            default -> false;
        };

        if (changed) {
            userRepository.save(user);
        }
    }

    private void validateWebhookToken(String accessToken) {
        String expectedToken = asaasProperties.getWebhookToken();
        if (expectedToken == null || expectedToken.isBlank()) {
            throw new WebhookUnauthorizedException("Webhook Asaas não configurado");
        }

        if (accessToken == null || !expectedToken.equals(accessToken)) {
            throw new WebhookUnauthorizedException("Webhook Asaas inválido");
        }
    }

    private boolean activateSubscription(AppUser user) {
        if (user.getPlan() == SubscriptionPlan.PRO && user.getSubscriptionStatus() == SubscriptionStatus.ACTIVE) {
            return false;
        }

        user.setPlan(SubscriptionPlan.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        user.setSubscriptionEndsAt(LocalDateTime.now().plusMonths(1));
        return true;
    }

    private boolean applyPastDue(AppUser user) {
        if (user.getSubscriptionStatus() == SubscriptionStatus.PAST_DUE) {
            return false;
        }

        user.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
        return true;
    }

    private boolean cancelSubscription(AppUser user) {
        if (user.getPlan() == SubscriptionPlan.FREE && user.getSubscriptionStatus() == SubscriptionStatus.CANCELED) {
            return false;
        }

        user.setSubscriptionStatus(SubscriptionStatus.CANCELED);
        user.setPlan(SubscriptionPlan.FREE);
        return true;
    }
}
