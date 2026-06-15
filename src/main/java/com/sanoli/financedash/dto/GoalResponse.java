package com.sanoli.financedash.dto;

import com.sanoli.financedash.domain.Goal;
import com.sanoli.financedash.domain.GoalType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record GoalResponse(
        UUID id,
        String title,
        Integer month,
        Integer year,
        BigDecimal targetAmount,
        GoalType type,
        UUID categoryId,
        String categoryName,
        String categoryColor,
        BigDecimal currentAmount,
        BigDecimal progressPercentage,
        LocalDateTime createdAt
) {
    public static GoalResponse fromEntity(Goal goal, BigDecimal currentAmount, BigDecimal progressPercentage) {
        return new GoalResponse(
                goal.getId(),
                goal.getTitle(),
                goal.getMonth(),
                goal.getYear(),
                goal.getTargetAmount(),
                goal.getType(),
                goal.getCategory() == null ? null : goal.getCategory().getId(),
                goal.getCategory() == null ? null : goal.getCategory().getName(),
                goal.getCategory() == null ? null : goal.getCategory().getColor(),
                currentAmount,
                progressPercentage,
                goal.getCreatedAt()
        );
    }
}

