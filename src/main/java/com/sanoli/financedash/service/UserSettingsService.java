package com.sanoli.financedash.service;

import com.sanoli.financedash.domain.DigestFrequency;
import com.sanoli.financedash.domain.UserSettings;
import com.sanoli.financedash.dto.UserSettingsRequest;
import com.sanoli.financedash.dto.UserSettingsResponse;
import com.sanoli.financedash.repository.UserSettingsRepository;
import com.sanoli.financedash.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final CurrentUserService currentUserService;

    public UserSettingsService(UserSettingsRepository userSettingsRepository, CurrentUserService currentUserService) {
        this.userSettingsRepository = userSettingsRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public UserSettingsResponse getForCurrentUser() {
        return UserSettingsResponse.fromEntity(getOrCreate(currentUserService.getCurrentUserId()));
    }

    @Transactional
    public UserSettingsResponse updateForCurrentUser(UserSettingsRequest request) {
        UserSettings settings = getOrCreate(currentUserService.getCurrentUserId());
        settings.setMonthlyIncomeGoal(request.monthlyIncomeGoal());
        settings.setMonthlyReserveTarget(request.monthlyReserveTarget());
        settings.setMonthlyFixedCost(request.monthlyFixedCost());
        settings.setBillableHoursPerMonth(request.billableHoursPerMonth());
        settings.setTaxRate(request.taxRate());
        settings.setDesiredMargin(request.desiredMargin());
        if (request.digestFrequency() != null) {
            settings.setDigestFrequency(request.digestFrequency());
        }
        return UserSettingsResponse.fromEntity(userSettingsRepository.save(settings));
    }

    @Transactional
    public UserSettings getOrCreate(UUID userId) {
        return userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserSettings settings = new UserSettings();
                    settings.setUserId(userId);
                    settings.setDigestFrequency(DigestFrequency.WEEKLY);
                    return userSettingsRepository.save(settings);
                });
    }
}
