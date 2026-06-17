package com.sanoli.financedash.controller;

import com.sanoli.financedash.dto.UserSettingsRequest;
import com.sanoli.financedash.dto.UserSettingsResponse;
import com.sanoli.financedash.service.UserSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user-settings")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    public UserSettingsController(UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
    }

    @GetMapping
    @Operation(summary = "Retorna o perfil financeiro do usuário autenticado")
    public ResponseEntity<UserSettingsResponse> get() {
        return ResponseEntity.ok(userSettingsService.getForCurrentUser());
    }

    @PutMapping
    @Operation(summary = "Atualiza o perfil financeiro do usuário autenticado")
    public ResponseEntity<UserSettingsResponse> update(@Valid @RequestBody UserSettingsRequest request) {
        return ResponseEntity.ok(userSettingsService.updateForCurrentUser(request));
    }
}
