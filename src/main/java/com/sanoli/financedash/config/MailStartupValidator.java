package com.sanoli.financedash.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MailStartupValidator implements ApplicationRunner {

    private static final Pattern ANGLE_BRACKET_ADDRESS = Pattern.compile("<([^<>]+)>");
    private static final Pattern EMAIL_ADDRESS = Pattern.compile("^[^\\s@<>]+@[^\\s@<>]+\\.[^\\s@<>]+$");

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

        if (usesResend()) {
            if (isBlank(mailProperties.getApiKey())) {
                missing.add("RESEND_API_KEY (app.mail.api-key)");
            }
        } else {
            if (isBlank(mailProperties.getHost())) {
                missing.add("SPRING_MAIL_HOST (spring.mail.host)");
            }
            if (isBlank(mailProperties.getUsername())) {
                missing.add("SPRING_MAIL_USERNAME (spring.mail.username)");
            }
            if (isBlank(mailProperties.getPassword())) {
                missing.add("SPRING_MAIL_PASSWORD (spring.mail.password)");
            }
        }

        if (!missing.isEmpty()) {
            String mode = usesResend() ? "Resend (HTTP API)" : "SMTP";
            throw new IllegalStateException(
                    "E-mail habilitado (MAIL_ENABLED=true), mas configuração " + mode + " incompleta. Defina: "
                            + String.join(", ", missing)
            );
        }

        validateFromAddress();
    }

    private void validateFromAddress() {
        String from = mailProperties.getFrom();
        if (isBlank(from)) {
            return;
        }

        String emailAddress = extractEmailAddress(from.trim());
        if (!EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            throw new IllegalStateException(
                    "MAIL_FROM invalido: \"" + from + "\". "
                            + "Use um e-mail completo, ex.: onboarding@resend.dev "
                            + "ou FinanceDash <onboarding@resend.dev>. "
                            + "No Railway, evite < > na variavel; prefira apenas o e-mail."
            );
        }

        if (usesResend() && (emailAddress.endsWith(".railway.app") || emailAddress.contains("up.railway.app"))) {
            throw new IllegalStateException(
                    "MAIL_FROM nao pode usar dominio railway.app no Resend. "
                            + "Para testes use onboarding@resend.dev; "
                            + "para producao verifique seu proprio dominio em https://resend.com/domains."
            );
        }
    }

    static String extractEmailAddress(String from) {
        Matcher matcher = ANGLE_BRACKET_ADDRESS.matcher(from);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return from.trim();
    }

    private boolean usesResend() {
        return !"smtp".equalsIgnoreCase(mailProperties.getProvider());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
