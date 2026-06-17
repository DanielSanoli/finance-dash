package com.sanoli.financedash.service;

import com.sanoli.financedash.domain.Alert;
import com.sanoli.financedash.dto.AlertResponse;
import com.sanoli.financedash.exception.ResourceNotFoundException;
import com.sanoli.financedash.repository.AlertRepository;
import com.sanoli.financedash.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final CurrentUserService currentUserService;

    public AlertService(AlertRepository alertRepository, CurrentUserService currentUserService) {
        this.alertRepository = alertRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> listForCurrentUser(boolean unreadOnly) {
        UUID userId = currentUserService.getCurrentUserId();
        List<Alert> alerts = unreadOnly
                ? alertRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                : alertRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return alerts.stream().map(AlertResponse::fromEntity).toList();
    }

    @Transactional
    public AlertResponse markAsRead(UUID id) {
        UUID userId = currentUserService.getCurrentUserId();
        Alert alert = alertRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert não encontrado: " + id));
        alert.setRead(true);
        return AlertResponse.fromEntity(alertRepository.save(alert));
    }
}
