package com.sanoli.financedash.service;

import com.sanoli.financedash.config.AuthProperties;
import com.sanoli.financedash.domain.AppUser;
import com.sanoli.financedash.domain.RefreshToken;
import com.sanoli.financedash.domain.TokenPurpose;
import com.sanoli.financedash.domain.UserActionToken;
import com.sanoli.financedash.dto.AuthResponse;
import com.sanoli.financedash.dto.ForgotPasswordRequest;
import com.sanoli.financedash.dto.LoginRequest;
import com.sanoli.financedash.dto.MessageResponse;
import com.sanoli.financedash.dto.RefreshTokenRequest;
import com.sanoli.financedash.dto.RegisterRequest;
import com.sanoli.financedash.dto.ResetPasswordRequest;
import com.sanoli.financedash.dto.UserResponse;
import com.sanoli.financedash.exception.BusinessException;
import com.sanoli.financedash.repository.RefreshTokenRepository;
import com.sanoli.financedash.repository.UserActionTokenRepository;
import com.sanoli.financedash.repository.UserRepository;
import com.sanoli.financedash.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserActionTokenRepository userActionTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DefaultCategoryService defaultCategoryService;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final LoginRateLimiter loginRateLimiter;
    private final String publicBaseUrl;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            UserActionTokenRepository userActionTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            DefaultCategoryService defaultCategoryService,
            TokenService tokenService,
            EmailService emailService,
            LoginRateLimiter loginRateLimiter,
            @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userActionTokenRepository = userActionTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.defaultCategoryService = defaultCategoryService;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.loginRateLimiter = loginRateLimiter;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Já existe um usuário com este email");
        }

        AppUser user = new AppUser();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        AppUser savedUser = userRepository.save(user);
        defaultCategoryService.seedForUser(savedUser);

        String verificationToken = tokenService.createEmailVerificationToken(savedUser);
        emailService.sendEmailVerification(email, publicBaseUrl + "/?verify=" + verificationToken);

        return toAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        loginRateLimiter.checkAllowed(email);

        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    loginRateLimiter.registerFailure(email);
                    return new BusinessException("Email ou senha inválidos");
                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginRateLimiter.registerFailure(email);
            throw new BusinessException("Email ou senha inválidos");
        }

        loginRateLimiter.reset(email);
        return toAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(request.refreshToken())
                .orElseThrow(() -> new BusinessException("Refresh token inválido"));

        if (refreshToken.isExpired()) {
            refreshToken.setRevoked(true);
            throw new BusinessException("Refresh token expirado");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        return toAuthResponse(refreshToken.getUser());
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(request.email())).ifPresent(user -> {
            String token = tokenService.createPasswordResetToken(user);
            emailService.sendPasswordResetEmail(user.getEmail(), publicBaseUrl + "/?reset=" + token);
        });
        return new MessageResponse("Se o email existir, enviaremos instruções de recuperação.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        UserActionToken token = userActionTokenRepository
                .findByTokenAndPurposeAndUsedFalse(request.token(), TokenPurpose.PASSWORD_RESET)
                .orElseThrow(() -> new BusinessException("Token de recuperação inválido"));

        if (token.isExpired()) {
            throw new BusinessException("Token de recuperação expirado");
        }

        AppUser user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        token.setUsed(true);
        userRepository.save(user);
        userActionTokenRepository.save(token);
        return new MessageResponse("Senha atualizada com sucesso.");
    }

    @Transactional
    public MessageResponse verifyEmail(String tokenValue) {
        UserActionToken token = userActionTokenRepository
                .findByTokenAndPurposeAndUsedFalse(tokenValue, TokenPurpose.EMAIL_VERIFICATION)
                .orElseThrow(() -> new BusinessException("Token de verificação inválido"));

        if (token.isExpired()) {
            throw new BusinessException("Token de verificação expirado");
        }

        AppUser user = token.getUser();
        user.setEmailVerified(true);
        token.setUsed(true);
        userRepository.save(user);
        userActionTokenRepository.save(token);
        return new MessageResponse("Email verificado com sucesso.");
    }

    private AuthResponse toAuthResponse(AppUser user) {
        String refreshToken = tokenService.createRefreshToken(user);
        return AuthResponse.bearer(jwtService.generateToken(user), refreshToken, UserResponse.fromEntity(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
