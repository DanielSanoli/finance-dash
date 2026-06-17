package com.sanoli.financedash.controller;

import com.sanoli.financedash.domain.AppUser;
import com.sanoli.financedash.repository.UserRepository;
import com.sanoli.financedash.security.CurrentUserService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/auth")
@Profile("test")
public class TestAuthController {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public TestAuthController(CurrentUserService currentUserService, UserRepository userRepository) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    @PostMapping("/simulate-expired-trial")
    public ResponseEntity<Void> simulateExpiredTrial() {
        AppUser user = currentUserService.getCurrentUser();
        user.setTrialEndsAt(LocalDateTime.now().minusDays(1));
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }
}
