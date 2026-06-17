package com.sanoli.financedash.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private int loginMaxAttempts = 5;
    private int loginWindowMinutes = 15;
    private int passwordResetExpirationHours = 2;
    private int emailVerificationExpirationHours = 48;
    private int refreshTokenExpirationDays = 30;

    public int getLoginMaxAttempts() {
        return loginMaxAttempts;
    }

    public void setLoginMaxAttempts(int loginMaxAttempts) {
        this.loginMaxAttempts = loginMaxAttempts;
    }

    public int getLoginWindowMinutes() {
        return loginWindowMinutes;
    }

    public void setLoginWindowMinutes(int loginWindowMinutes) {
        this.loginWindowMinutes = loginWindowMinutes;
    }

    public int getPasswordResetExpirationHours() {
        return passwordResetExpirationHours;
    }

    public void setPasswordResetExpirationHours(int passwordResetExpirationHours) {
        this.passwordResetExpirationHours = passwordResetExpirationHours;
    }

    public int getEmailVerificationExpirationHours() {
        return emailVerificationExpirationHours;
    }

    public void setEmailVerificationExpirationHours(int emailVerificationExpirationHours) {
        this.emailVerificationExpirationHours = emailVerificationExpirationHours;
    }

    public int getRefreshTokenExpirationDays() {
        return refreshTokenExpirationDays;
    }

    public void setRefreshTokenExpirationDays(int refreshTokenExpirationDays) {
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }
}
