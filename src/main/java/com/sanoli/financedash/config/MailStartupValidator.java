package com.sanoli.financedash.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MailStartupValidator implements ApplicationRunner {

    private final MailProperties mailProperties;

    public MailStartupValidator(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!mailProperties.isEnabled()) {
            return;
        }

        validateRequiredConfiguration();
    }

    void validateRequiredConfiguration() {
        List<String> missing = new ArrayList<>();

        if (isBlank(mailProperties.getFrom())) {
            missing.add("MAIL_FROM (app.mail.from)");
        }
        if (isBlank(mailProperties.getHost())) {
            missing.add("SPRING_MAIL_HOST (spring.mail.host)");
        }
        if (isBlank(mailProperties.getUsername())) {
            missing.add("SPRING_MAIL_USERNAME (spring.mail.username)");
        }
        if (isBlank(mailProperties.getPassword())) {
            missing.add("SPRING_MAIL_PASSWORD (spring.mail.password)");
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "E-mail habilitado (MAIL_ENABLED=true), mas configuração SMTP incompleta. Defina: "
                            + String.join(", ", missing)
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
