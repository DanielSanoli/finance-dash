package com.sanoli.financedash.service;

import com.sanoli.financedash.config.AuthProperties;
import com.sanoli.financedash.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiter {

    private final AuthProperties authProperties;
    private final Map<String, AttemptWindow> attemptsByEmail = new ConcurrentHashMap<>();

    public LoginRateLimiter(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public void checkAllowed(String email) {
        AttemptWindow window = attemptsByEmail.computeIfAbsent(normalize(email), key -> new AttemptWindow());
        synchronized (window) {
            window.refreshIfExpired(authProperties.getLoginWindowMinutes());
            if (window.count >= authProperties.getLoginMaxAttempts()) {
                throw new BusinessException("Muitas tentativas de login. Tente novamente mais tarde.");
            }
        }
    }

    public void registerFailure(String email) {
        AttemptWindow window = attemptsByEmail.computeIfAbsent(normalize(email), key -> new AttemptWindow());
        synchronized (window) {
            window.refreshIfExpired(authProperties.getLoginWindowMinutes());
            window.count++;
        }
    }

    public void reset(String email) {
        attemptsByEmail.remove(normalize(email));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private static final class AttemptWindow {
        private int count;
        private LocalDateTime windowStart = LocalDateTime.now();

        private void refreshIfExpired(int windowMinutes) {
            if (windowStart.plusMinutes(windowMinutes).isBefore(LocalDateTime.now())) {
                count = 0;
                windowStart = LocalDateTime.now();
            }
        }
    }
}
