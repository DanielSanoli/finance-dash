package com.sanoli.financedash.repository;

import com.sanoli.financedash.domain.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByUserIdAndMonthAndYear(UUID userId, Integer month, Integer year);

    List<Goal> findAllByUserId(UUID userId);

    Optional<Goal> findByIdAndUserId(UUID id, UUID userId);
}

