package com.sanoli.financedash.service;

import com.sanoli.financedash.domain.AppUser;
import com.sanoli.financedash.dto.AuthResponse;
import com.sanoli.financedash.dto.ForgotPasswordRequest;
import com.sanoli.financedash.dto.LoginRequest;
import com.sanoli.financedash.dto.RegisterRequest;
import com.sanoli.financedash.exception.BusinessException;
import com.sanoli.financedash.repository.RefreshTokenRepository;
import com.sanoli.financedash.repository.UserActionTokenRepository;
import com.sanoli.financedash.repository.UserRepository;
import com.sanoli.financedash.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserActionTokenRepository userActionTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private DefaultCategoryService defaultCategoryService;

    @Mock
    private TokenService tokenService;

    @Mock
    private EmailService emailService;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                userActionTokenRepository,
                passwordEncoder,
                jwtService,
                defaultCategoryService,
                tokenService,
                emailService,
                loginRateLimiter,
                "http://localhost:8080"
        );
    }

    @Test
    void shouldRegisterUserAndSeedDefaultCategories() {
        RegisterRequest request = new RegisterRequest("Daniel", "DANIEL@EXAMPLE.COM", "password123");
        when(userRepository.existsByEmailIgnoreCase("daniel@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hash");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(tokenService.createEmailVerificationToken(any(AppUser.class))).thenReturn("verify-token");
        when(tokenService.createRefreshToken(any(AppUser.class))).thenReturn("refresh-token");
        when(jwtService.generateToken(any(AppUser.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().email()).isEqualTo("daniel@example.com");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash");
        verify(defaultCategoryService).seedForUser(any(AppUser.class));
        verify(emailService).sendEmailVerification("daniel@example.com", "http://localhost:8080/?verify=verify-token");
    }

    @Test
    void shouldRejectDuplicatedEmail() {
        RegisterRequest request = new RegisterRequest("Daniel", "daniel@example.com", "password123");
        when(userRepository.existsByEmailIgnoreCase("daniel@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("usuário");
    }

    @Test
    void shouldLoginWithValidCredentials() {
        AppUser user = user();
        when(userRepository.findByEmailIgnoreCase("daniel@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);
        when(tokenService.createRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("daniel@example.com", "password123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().id()).isEqualTo(user.getId());
        verify(loginRateLimiter).reset("daniel@example.com");
    }

    @Test
    void shouldRejectInvalidPassword() {
        AppUser user = user();
        when(userRepository.findByEmailIgnoreCase("daniel@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("daniel@example.com", "wrong")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email ou senha inválidos");
    }

    @Test
    void shouldReturnGenericMessageWhenPasswordResetEmailFails() {
        AppUser user = user();
        when(userRepository.findByEmailIgnoreCase("daniel@example.com")).thenReturn(Optional.of(user));
        when(tokenService.createPasswordResetToken(user)).thenReturn("reset-token");
        doThrow(new BusinessException("Não foi possível enviar o e-mail. Tente novamente mais tarde."))
                .when(emailService)
                .sendPasswordResetEmail("daniel@example.com", "http://localhost:8080/?reset=reset-token");

        var response = authService.forgotPassword(new ForgotPasswordRequest("daniel@example.com"));

        assertThat(response.message()).contains("Se o email existir");
        verify(tokenService).createPasswordResetToken(user);
    }

    @Test
    void shouldReturnGenericMessageWhenEmailDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        var response = authService.forgotPassword(new ForgotPasswordRequest("missing@example.com"));

        assertThat(response.message()).contains("Se o email existir");
    }

    private AppUser user() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setName("Daniel");
        user.setEmail("daniel@example.com");
        user.setPasswordHash("hash");
        return user;
    }
}
