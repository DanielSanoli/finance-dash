package com.sanoli.financedash.service;

import com.sanoli.financedash.domain.AppUser;
import com.sanoli.financedash.dto.AuthResponse;
import com.sanoli.financedash.dto.LoginRequest;
import com.sanoli.financedash.dto.RegisterRequest;
import com.sanoli.financedash.dto.UserResponse;
import com.sanoli.financedash.exception.BusinessException;
import com.sanoli.financedash.repository.UserRepository;
import com.sanoli.financedash.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DefaultCategoryService defaultCategoryService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            DefaultCategoryService defaultCategoryService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.defaultCategoryService = defaultCategoryService;
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
        return toAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new BusinessException("Email ou senha inválidos"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("Email ou senha inválidos");
        }

        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(AppUser user) {
        return AuthResponse.bearer(jwtService.generateToken(user), UserResponse.fromEntity(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
