package com.sanoli.financedash.exception;

public class WebhookUnauthorizedException extends RuntimeException {

    public WebhookUnauthorizedException(String message) {
        super(message);
    }
}
