package com.sanoli.financedash.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.asaas")
public class AsaasProperties {

    private boolean enabled = false;
    private String apiKey = "";
    private String baseUrl = "https://api-sandbox.asaas.com/v3";
    private String webhookToken = "";
    private BigDecimal proPlanValue = new BigDecimal("29.90");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getWebhookToken() {
        return webhookToken;
    }

    public void setWebhookToken(String webhookToken) {
        this.webhookToken = webhookToken;
    }

    public BigDecimal getProPlanValue() {
        return proPlanValue;
    }

    public void setProPlanValue(BigDecimal proPlanValue) {
        this.proPlanValue = proPlanValue;
    }
}
