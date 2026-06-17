package com.sanoli.financedash.service;

public interface EmailService {

    void sendPasswordResetEmail(String email, String resetUrl);

    void sendEmailVerification(String email, String verificationUrl);

    void sendRadarDigest(String email, String subject, String body);

    void sendCriticalAlert(String email, String subject, String body);
}
