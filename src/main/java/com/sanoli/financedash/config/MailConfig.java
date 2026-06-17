package com.sanoli.financedash.config;

import com.sanoli.financedash.service.EmailService;
import com.sanoli.financedash.service.LoggingEmailService;
import com.sanoli.financedash.service.ResendEmailService;
import com.sanoli.financedash.service.SmtpEmailService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class MailConfig {

    @Configuration
    @ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
    static class EnabledMailConfiguration {

        @Bean
        @ConditionalOnProperty(name = "app.mail.provider", havingValue = "resend", matchIfMissing = true)
        EmailService resendEmailService(MailProperties mailProperties) {
            return new ResendEmailService(mailProperties);
        }

        @Bean
        @ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp")
        EmailService smtpEmailService(JavaMailSender mailSender, MailProperties mailProperties) {
            return new SmtpEmailService(mailSender, mailProperties);
        }
    }

    @Bean
    @ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
    EmailService loggingEmailService() {
        return new LoggingEmailService();
    }
}
