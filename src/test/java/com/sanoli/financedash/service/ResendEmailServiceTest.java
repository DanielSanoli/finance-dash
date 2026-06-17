package com.sanoli.financedash.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import com.sanoli.financedash.config.MailProperties;
import com.sanoli.financedash.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResendEmailServiceTest {

    @Mock
    private Resend resend;

    @Mock
    private com.resend.services.emails.Emails emails;

    private ResendEmailService emailService;

    @BeforeEach
    void setUp() {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setFrom("FinanceDash <onboarding@resend.dev>");
        emailService = new ResendEmailService(resend, mailProperties);
    }

    @Test
    void shouldSendVerificationEmail() throws ResendException {
        when(resend.emails()).thenReturn(emails);
        when(emails.send(any(CreateEmailOptions.class))).thenReturn(new CreateEmailResponse("email-id"));

        emailService.sendEmailVerification("user@example.com", "https://app.example.com/?verify=token");

        verify(emails).send(any(CreateEmailOptions.class));
    }

    @Test
    void shouldWrapResendFailuresAsBusinessException() throws ResendException {
        when(resend.emails()).thenReturn(emails);
        when(emails.send(any(CreateEmailOptions.class))).thenThrow(new ResendException("api down", null));

        assertThatThrownBy(() -> emailService.sendPasswordResetEmail("user@example.com", "https://reset"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Não foi possível enviar o e-mail");
    }
}
