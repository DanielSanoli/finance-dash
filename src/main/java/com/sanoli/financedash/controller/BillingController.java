package com.sanoli.financedash.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sanoli.financedash.dto.CheckoutResponse;
import com.sanoli.financedash.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/checkout/pro")
    @Operation(summary = "Inicia checkout do plano Pro via Asaas")
    public ResponseEntity<CheckoutResponse> checkoutPro() {
        return ResponseEntity.ok(billingService.createProCheckout());
    }

    @PostMapping("/webhook")
    @Operation(summary = "Recebe eventos de pagamento do Asaas")
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "asaas-access-token", required = false) String accessToken,
            @RequestBody JsonNode payload
    ) {
        billingService.handleWebhook(accessToken, payload);
        return ResponseEntity.ok().build();
    }
}
