package com.sanoli.financedash.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MailStartupValidatorTest {

    private MailProperties mailProperties;
    private MailStartupValidator validator;

    @BeforeEach
    void setUp() {
        mailProperties = new MailProperties();
        mailProperties.setEnabled(true);
        mailProperties.setProvider("smtp");
        mailProperties.setFrom("noreply@financedash.com");
        mailProperties.setHost("smtp.example.com");
        mailProperties.setUsername("smtp-user");
        mailProperties.setPassword("smtp-pass");
        validator = new MailStartupValidator(mailProperties);
    }

    @Test
    void shouldSkipValidationWhenMailIsDisabled() {
        mailProperties.setEnabled(false);
        mailProperties.setHost("");

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }

    @Test
    void shouldFailWhenSmtpSettingsAreMissing() {
        mailProperties.setPassword("");

        assertThatThrownBy(validator::validateRequiredConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SPRING_MAIL_PASSWORD")
                .hasMessageContaining("configuração SMTP incompleta");
    }

    @Test
    void shouldPassWhenAllRequiredSettingsArePresent() {
        assertThatCode(validator::validateRequiredConfiguration).doesNotThrowAnyException();
    }

    @Test
    void shouldFailWhenResendApiKeyIsMissing() {
        mailProperties.setProvider("resend");
        mailProperties.setHost("");
        mailProperties.setUsername("");
        mailProperties.setPassword("");
        mailProperties.setApiKey("");

        assertThatThrownBy(validator::validateRequiredConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RESEND_API_KEY")
                .hasMessageContaining("Resend (HTTP API)");
    }

    @Test
    void shouldPassWhenResendSettingsArePresent() {
        mailProperties.setProvider("resend");
        mailProperties.setApiKey("re_test_key");

        assertThatCode(validator::validateRequiredConfiguration).doesNotThrowAnyException();
    }
}
