package com.sanoli.financedash.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.sanoli.financedash.config.MailProperties;
import com.sanoli.financedash.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResendEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);

    private final Resend resend;
    private final MailProperties mailProperties;

    public ResendEmailService(MailProperties mailProperties) {
        this(new Resend(mailProperties.getApiKey()), mailProperties);
    }

    ResendEmailService(Resend resend, MailProperties mailProperties) {
        this.resend = resend;
        this.mailProperties = mailProperties;
    }

    @Override
    public void sendPasswordResetEmail(String email, String resetUrl) {
        send(
                email,
                "Redefina sua senha no FinanceDash",
                """
                        Olá,

                        Recebemos um pedido para redefinir sua senha no FinanceDash.
                        Acesse o link abaixo para continuar (válido por tempo limitado):

                        %s

                        Se você não solicitou isso, ignore este e-mail.
                        """.formatted(resetUrl)
        );
    }

    @Override
    public void sendEmailVerification(String email, String verificationUrl) {
        send(
                email,
                "Confirme seu e-mail no FinanceDash",
                """
                        Olá,

                        Bem-vindo(a) ao FinanceDash! Confirme seu e-mail para ativar sua conta:

                        %s

                        Se você não criou esta conta, ignore este e-mail.
                        """.formatted(verificationUrl)
        );
    }

    @Override
    public void sendRadarDigest(String email, String subject, String body) {
        send(email, subject, body);
    }

    @Override
    public void sendCriticalAlert(String email, String subject, String body) {
        send(email, subject, body);
    }

    private void send(String to, String subject, String text) {
        try {
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(mailProperties.getFrom())
                    .to(to)
                    .subject(subject)
                    .text(text)
                    .build();
            resend.emails().send(params);
            log.info("E-mail enviado via Resend para conta terminando em {}", maskEmail(to));
        } catch (Exception exception) {
            log.error("Falha ao enviar e-mail via Resend para conta terminando em {}", maskEmail(to), exception);
            throw new BusinessException("Não foi possível enviar o e-mail. Tente novamente mais tarde.");
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        return "*" + email.substring(email.indexOf('@'));
    }
}
