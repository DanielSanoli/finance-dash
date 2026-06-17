package com.sanoli.financedash.service;

import com.sanoli.financedash.config.MailProperties;
import com.sanoli.financedash.exception.BusinessException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    private SmtpEmailService emailService;

    @BeforeEach
    void setUp() {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setFrom("noreply@financedash.com");
        emailService = new SmtpEmailService(mailSender, mailProperties);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void shouldSendVerificationEmail() {
        emailService.sendEmailVerification("user@example.com", "https://app.example.com/?verify=token");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void shouldWrapMessagingFailuresAsBusinessException() {
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendPasswordResetEmail("user@example.com", "https://reset"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Não foi possível enviar o e-mail");
    }
}
