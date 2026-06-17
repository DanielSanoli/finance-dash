package com.sanoli.financedash.service;

public interface EmailService {

    void sendPasswordResetEmail(String email, String resetUrl);

    void sendEmailVerification(String email, String verificationUrl);
}
