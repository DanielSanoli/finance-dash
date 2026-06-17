package com.sanoli.financedash.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class AsaasStartupValidator implements ApplicationRunner {

    static final String DEFAULT_JWT_SECRET = "finance-dash-dev-secret-change-me";

    private static final Logger log = LoggerFactory.getLogger(AsaasStartupValidator.class);

    private final AsaasProperties asaasProperties;
    private final String jwtSecret;
    private final String publicBaseUrl;

    public AsaasStartupValidator(
            AsaasProperties asaasProperties,
            @Value("${app.security.jwt.secret}") String jwtSecret,
            @Value("${app.public-base-url}") String publicBaseUrl
    ) {
        this.asaasProperties = asaasProperties;
        this.jwtSecret = jwtSecret;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!asaasProperties.isEnabled()) {
            return;
        }

        validateRequiredConfiguration();
        warnIfSandboxBaseUrl();
        warnIfInsecureDefaults();
    }

    void validateRequiredConfiguration() {
        List<String> missing = new ArrayList<>();

        if (isBlank(asaasProperties.getApiKey())) {
            missing.add("ASAAS_API_KEY (app.asaas.api-key)");
        }
        if (isBlank(asaasProperties.getBaseUrl())) {
            missing.add("ASAAS_BASE_URL (app.asaas.base-url)");
        }
        if (isBlank(asaasProperties.getWebhookToken())) {
            missing.add("ASAAS_WEBHOOK_TOKEN (app.asaas.webhook-token)");
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Billing Asaas habilitado (ASAAS_ENABLED=true), mas configuração incompleta. Defina: "
                            + String.join(", ", missing)
            );
        }
    }

    private void warnIfSandboxBaseUrl() {
        String baseUrl = asaasProperties.getBaseUrl().toLowerCase(Locale.ROOT);
        if (baseUrl.contains("sandbox")) {
            log.warn(
                    "ASAAS_ENABLED=true com base-url apontando para sandbox ({}). "
                            + "Use a URL de produção da Asaas antes de cobrar clientes reais.",
                    asaasProperties.getBaseUrl()
            );
        }
    }

    private void warnIfInsecureDefaults() {
        if (DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            log.warn(
                    "Billing habilitado com JWT_SECRET no valor padrão de desenvolvimento. "
                            + "Defina um segredo forte em produção."
            );
        }

        if (isLocalhost(publicBaseUrl)) {
            log.warn(
                    "Billing habilitado com PUBLIC_BASE_URL apontando para localhost ({}). "
                            + "Links de e-mail e callbacks podem falhar em produção.",
                    publicBaseUrl
            );
        }
    }

    private boolean isLocalhost(String value) {
        if (isBlank(value)) {
            return false;
        }

        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("localhost") || normalized.contains("127.0.0.1");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
