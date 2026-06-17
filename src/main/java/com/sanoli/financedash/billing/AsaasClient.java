package com.sanoli.financedash.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.sanoli.financedash.config.AsaasProperties;
import com.sanoli.financedash.exception.BusinessException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class AsaasClient {

    private final AsaasProperties asaasProperties;
    private final RestClient restClient;

    public AsaasClient(AsaasProperties asaasProperties, RestClient.Builder restClientBuilder) {
        this.asaasProperties = asaasProperties;
        this.restClient = restClientBuilder
                .baseUrl(asaasProperties.getBaseUrl())
                .defaultHeader("access_token", asaasProperties.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String createCustomer(String name, String email) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("email", email);
        payload.put("notificationDisabled", true);

        JsonNode response = post("/customers", payload);
        return requiredText(response, "id");
    }

    public AsaasSubscriptionResult createSubscription(String customerId, BigDecimal value) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("customer", customerId);
        payload.put("billingType", "UNDEFINED");
        payload.put("cycle", "MONTHLY");
        payload.put("value", value);
        payload.put("description", "FinanceDash Pro");

        JsonNode response = post("/subscriptions", payload);
        return new AsaasSubscriptionResult(
                requiredText(response, "id"),
                textOrNull(response, "invoiceUrl")
        );
    }

    private JsonNode post(String path, Map<String, Object> payload) {
        if (!asaasProperties.isEnabled()) {
            throw new BusinessException("Integração Asaas não configurada");
        }

        return restClient.post()
                .uri(path)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new BusinessException("Resposta inválida do Asaas");
        }
        return value.asText();
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    public record AsaasSubscriptionResult(String subscriptionId, String checkoutUrl) {
    }
}
