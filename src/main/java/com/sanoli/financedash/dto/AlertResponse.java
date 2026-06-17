package com.sanoli.financedash.dto;

import com.sanoli.financedash.domain.Alert;
import com.sanoli.financedash.domain.AlertType;
import com.sanoli.financedash.domain.Severity;

import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        AlertType type,
        Severity severity,
        String message,
        String actionSuggestion,
        String dataSnapshot,
        Instant createdAt,
        boolean read
) {
    public static AlertResponse fromEntity(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getType(),
                alert.getSeverity(),
                alert.getMessage(),
                alert.getActionSuggestion(),
                alert.getDataSnapshot(),
                alert.getCreatedAt(),
                alert.isRead()
        );
    }
}
