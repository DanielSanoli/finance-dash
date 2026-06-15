package com.sanoli.financedash.repository;

import com.sanoli.financedash.domain.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByMonthAndYear(Integer month, Integer year);
}

