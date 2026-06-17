package com.sanoli.financedash.repository;

import com.sanoli.financedash.domain.Alert;
import com.sanoli.financedash.domain.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    List<Alert> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Alert> findByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId);

    Optional<Alert> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndTypeAndReadFalseAndCreatedAtAfter(UUID userId, AlertType type, Instant after);
}
