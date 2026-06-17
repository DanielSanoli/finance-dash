package com.sanoli.financedash.service;

import com.sanoli.financedash.config.AuthProperties;
import com.sanoli.financedash.domain.AppUser;
import com.sanoli.financedash.domain.RefreshToken;
import com.sanoli.financedash.domain.TokenPurpose;
import com.sanoli.financedash.domain.UserActionToken;
import com.sanoli.financedash.repository.RefreshTokenRepository;
import com.sanoli.financedash.repository.UserActionTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class TokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserActionTokenRepository userActionTokenRepository;
    private final AuthProperties authProperties;

    public TokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserActionTokenRepository userActionTokenRepository,
            AuthProperties authProperties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userActionTokenRepository = userActionTokenRepository;
        this.authProperties = authProperties;
    }

    @Transactional
    public String createRefreshToken(AppUser user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(generateToken());
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(authProperties.getRefreshTokenExpirationDays()));
        return refreshTokenRepository.save(refreshToken).getToken();
    }

    @Transactional
    public String createPasswordResetToken(AppUser user) {
        return createActionToken(user, TokenPurpose.PASSWORD_RESET, authProperties.getPasswordResetExpirationHours());
    }

    @Transactional
    public String createEmailVerificationToken(AppUser user) {
        return createActionToken(user, TokenPurpose.EMAIL_VERIFICATION, authProperties.getEmailVerificationExpirationHours());
    }

    private String createActionToken(AppUser user, TokenPurpose purpose, int expirationHours) {
        UserActionToken token = new UserActionToken();
        token.setUser(user);
        token.setPurpose(purpose);
        token.setToken(generateToken());
        token.setExpiresAt(LocalDateTime.now().plusHours(expirationHours));
        return userActionTokenRepository.save(token).getToken();
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
