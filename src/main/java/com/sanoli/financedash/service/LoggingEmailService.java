package com.sanoli.financedash.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);

    @Override
    public void sendPasswordResetEmail(String email, String resetUrl) {
        log.info("Password reset requested for account ending with {}. Reset link: {}", maskEmail(email), resetUrl);
    }

    @Override
    public void sendEmailVerification(String email, String verificationUrl) {
        log.info("Email verification requested for account ending with {}. Verification link: {}", maskEmail(email), verificationUrl);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        return email.substring(email.indexOf('@') - 1);
    }
}
