package com.sanoli.financedash.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(precision = 19, scale = 2)
    private BigDecimal monthlyIncomeGoal;

    @Column(precision = 19, scale = 2)
    private BigDecimal monthlyReserveTarget;

    @Column(precision = 19, scale = 2)
    private BigDecimal monthlyFixedCost;

    @Column(precision = 19, scale = 2)
    private BigDecimal billableHoursPerMonth;

    @Column(precision = 9, scale = 4)
    private BigDecimal taxRate;

    @Column(precision = 9, scale = 4)
    private BigDecimal desiredMargin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'WEEKLY'")
    private DigestFrequency digestFrequency = DigestFrequency.WEEKLY;

    private Instant lastDigestSentAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public BigDecimal getMonthlyIncomeGoal() {
        return monthlyIncomeGoal;
    }

    public void setMonthlyIncomeGoal(BigDecimal monthlyIncomeGoal) {
        this.monthlyIncomeGoal = monthlyIncomeGoal;
    }

    public BigDecimal getMonthlyReserveTarget() {
        return monthlyReserveTarget;
    }

    public void setMonthlyReserveTarget(BigDecimal monthlyReserveTarget) {
        this.monthlyReserveTarget = monthlyReserveTarget;
    }

    public BigDecimal getMonthlyFixedCost() {
        return monthlyFixedCost;
    }

    public void setMonthlyFixedCost(BigDecimal monthlyFixedCost) {
        this.monthlyFixedCost = monthlyFixedCost;
    }

    public BigDecimal getBillableHoursPerMonth() {
        return billableHoursPerMonth;
    }

    public void setBillableHoursPerMonth(BigDecimal billableHoursPerMonth) {
        this.billableHoursPerMonth = billableHoursPerMonth;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getDesiredMargin() {
        return desiredMargin;
    }

    public void setDesiredMargin(BigDecimal desiredMargin) {
        this.desiredMargin = desiredMargin;
    }

    public DigestFrequency getDigestFrequency() {
        return digestFrequency;
    }

    public void setDigestFrequency(DigestFrequency digestFrequency) {
        this.digestFrequency = digestFrequency;
    }

    public Instant getLastDigestSentAt() {
        return lastDigestSentAt;
    }

    public void setLastDigestSentAt(Instant lastDigestSentAt) {
        this.lastDigestSentAt = lastDigestSentAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
