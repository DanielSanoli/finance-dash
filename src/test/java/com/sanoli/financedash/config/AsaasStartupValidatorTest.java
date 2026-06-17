package com.sanoli.financedash.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AsaasStartupValidatorTest {

    private AsaasProperties asaasProperties;
    private AsaasStartupValidator validator;

    @BeforeEach
    void setUp() {
        asaasProperties = new AsaasProperties();
        asaasProperties.setEnabled(true);
        asaasProperties.setApiKey("api-key");
        asaasProperties.setBaseUrl("https://api.asaas.com/v3");
        asaasProperties.setWebhookToken("webhook-token");
        validator = new AsaasStartupValidator(
                asaasProperties,
                "production-secret",
                "https://app.financedash.com"
        );
    }

    @Test
    void shouldSkipValidationWhenBillingIsDisabled() {
        asaasProperties.setEnabled(false);
        asaasProperties.setApiKey("");
        asaasProperties.setWebhookToken("");

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }

    @Test
    void shouldFailWhenWebhookTokenIsMissing() {
        asaasProperties.setWebhookToken("");

        assertThatThrownBy(validator::validateRequiredConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ASAAS_WEBHOOK_TOKEN")
                .hasMessageContaining("configuração incompleta");
    }

    @Test
    void shouldFailWhenApiKeyOrBaseUrlAreMissing() {
        asaasProperties.setApiKey("");
        asaasProperties.setBaseUrl(" ");

        assertThatThrownBy(validator::validateRequiredConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ASAAS_API_KEY")
                .hasMessageContaining("ASAAS_BASE_URL");
    }

    @Test
    void shouldPassWhenAllRequiredSettingsArePresent() {
        assertThatCode(validator::validateRequiredConfiguration).doesNotThrowAnyException();
    }
}
