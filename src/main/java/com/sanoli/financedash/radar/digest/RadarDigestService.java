package com.sanoli.financedash.radar.digest;

import com.sanoli.financedash.config.RadarDigestProperties;
import com.sanoli.financedash.domain.Alert;
import com.sanoli.financedash.domain.AppUser;
import com.sanoli.financedash.domain.DigestFrequency;
import com.sanoli.financedash.domain.UserSettings;
import com.sanoli.financedash.radar.engine.RadarEngineService;
import com.sanoli.financedash.repository.UserRepository;
import com.sanoli.financedash.repository.UserSettingsRepository;
import com.sanoli.financedash.service.EmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class RadarDigestService {

    private final RadarEngineService radarEngineService;
    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final EmailService emailService;
    private final RadarDigestProperties radarDigestProperties;
    private final Clock clock;

    public RadarDigestService(
            RadarEngineService radarEngineService,
            UserRepository userRepository,
            UserSettingsRepository userSettingsRepository,
            EmailService emailService,
            RadarDigestProperties radarDigestProperties,
            Clock clock
    ) {
        this.radarEngineService = radarEngineService;
        this.userRepository = userRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.emailService = emailService;
        this.radarDigestProperties = radarDigestProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RadarDigestSnapshot buildSnapshot(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        int lastDayOfMonth = YearMonth.from(today).atEndOfMonth().getDayOfMonth();
        return RadarDigestSnapshot.of(
                radarEngineService.projectMonthBalance(userId),
                radarEngineService.safeToSpend(userId),
                radarEngineService.overdueReceivables(userId),
                lastDayOfMonth
        );
    }

    public boolean isDigestDue(UserSettings settings) {
        if (settings.getDigestFrequency() == null || settings.getDigestFrequency() == DigestFrequency.NONE) {
            return false;
        }

        Instant lastSent = settings.getLastDigestSentAt();
        if (lastSent == null) {
            return true;
        }

        Instant now = clock.instant();
        return switch (settings.getDigestFrequency()) {
            case DAILY -> lastSent.isBefore(LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant());
            case WEEKLY -> ChronoUnit.DAYS.between(lastSent, now) >= 7;
            case NONE -> false;
        };
    }

    @Transactional
    public void sendDigestIfDue(AppUser user) {
        if (!radarDigestProperties.isEnabled()) {
            return;
        }

        UserSettings settings = userSettingsRepository.findByUserId(user.getId()).orElse(null);
        if (settings == null || !isDigestDue(settings)) {
            return;
        }

        RadarDigestSnapshot snapshot = buildSnapshot(user.getId());
        String body = RadarDigestComposer.composeWeeklyDigest(snapshot);
        String subject = "Radar FinanceDash — resumo da semana";
        emailService.sendRadarDigest(user.getEmail(), subject, body);

        settings.setLastDigestSentAt(clock.instant());
        userSettingsRepository.save(settings);
    }

    @Transactional
    public void notifyCriticalAlert(UUID userId, Alert alert) {
        if (!radarDigestProperties.isEnabled()) {
            return;
        }

        AppUser user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        String body = RadarDigestComposer.composeCriticalAlert(alert);
        emailService.sendCriticalAlert(user.getEmail(), "Alerta crítico do Radar FinanceDash", body);
    }
}
