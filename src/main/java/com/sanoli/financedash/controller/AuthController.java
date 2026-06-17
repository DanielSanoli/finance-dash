package com.sanoli.financedash.controller;

import com.sanoli.financedash.dto.AuthResponse;
import com.sanoli.financedash.dto.ForgotPasswordRequest;
import com.sanoli.financedash.dto.LoginRequest;
import com.sanoli.financedash.dto.MessageResponse;
import com.sanoli.financedash.dto.RefreshTokenRequest;
import com.sanoli.financedash.dto.RegisterRequest;
import com.sanoli.financedash.dto.ResetPasswordRequest;
import com.sanoli.financedash.dto.UserResponse;
import com.sanoli.financedash.security.CurrentUserService;
import com.sanoli.financedash.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public AuthController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/register")
    @Operation(summary = "Cria uma conta de usuário")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica um usuário")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova o token JWT usando refresh token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicita recuperação de senha")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefine a senha com token de recuperação")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Confirma o email do usuário")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna o usuário autenticado")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(UserResponse.fromEntity(currentUserService.getCurrentUser()));
    }
}
