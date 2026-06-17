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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan plan = SubscriptionPlan.FREE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.TRIALING;

    @Column(nullable = false)
    private LocalDateTime trialEndsAt;

    private LocalDateTime subscriptionEndsAt;

    @Column(length = 64)
    private String asaasCustomerId;

    @Column(length = 64)
    private String asaasSubscriptionId;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (trialEndsAt == null) {
            trialEndsAt = now.plusDays(14);
        }
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }

    public SubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public LocalDateTime getTrialEndsAt() {
        return trialEndsAt;
    }

    public void setTrialEndsAt(LocalDateTime trialEndsAt) {
        this.trialEndsAt = trialEndsAt;
    }

    public LocalDateTime getSubscriptionEndsAt() {
        return subscriptionEndsAt;
    }

    public void setSubscriptionEndsAt(LocalDateTime subscriptionEndsAt) {
        this.subscriptionEndsAt = subscriptionEndsAt;
    }

    public String getAsaasCustomerId() {
        return asaasCustomerId;
    }

    public void setAsaasCustomerId(String asaasCustomerId) {
        this.asaasCustomerId = asaasCustomerId;
    }

    public String getAsaasSubscriptionId() {
        return asaasSubscriptionId;
    }

    public void setAsaasSubscriptionId(String asaasSubscriptionId) {
        this.asaasSubscriptionId = asaasSubscriptionId;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean hasActiveAccess() {
        LocalDateTime now = LocalDateTime.now();
        if (subscriptionStatus == SubscriptionStatus.ACTIVE) {
            return subscriptionEndsAt == null || subscriptionEndsAt.isAfter(now);
        }

        if (subscriptionStatus == SubscriptionStatus.TRIALING) {
            return trialEndsAt != null && trialEndsAt.isAfter(now);
        }

        return false;
    }

    public long getTrialDaysRemaining() {
        if (trialEndsAt == null || !trialEndsAt.isAfter(LocalDateTime.now())) {
            return 0;
        }
        return ChronoUnit.DAYS.between(LocalDateTime.now(), trialEndsAt);
    }

    public String getAccessMessage() {
        if (hasActiveAccess()) {
            return "Acesso ativo";
        }

        return switch (subscriptionStatus) {
            case PAST_DUE -> "Pagamento pendente";
            case CANCELED -> "Assinatura cancelada";
            case TRIALING -> "Trial expirado";
            case ACTIVE -> "Assinatura expirada";
        };
    }
}
