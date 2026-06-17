package com.sanoli.financedash.config;

import com.sanoli.financedash.service.EmailService;
import com.sanoli.financedash.service.LoggingEmailService;
import com.sanoli.financedash.service.SmtpEmailService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
    public EmailService smtpEmailService(JavaMailSender mailSender, MailProperties mailProperties) {
        return new SmtpEmailService(mailSender, mailProperties);
    }

    @Bean
    @ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
    public EmailService loggingEmailService() {
        return new LoggingEmailService();
    }
}
