package com.sanoli.financedash.radar.digest;

import com.sanoli.financedash.config.RadarDigestProperties;
import com.sanoli.financedash.domain.Alert;
import com.sanoli.financedash.domain.AlertType;
import com.sanoli.financedash.domain.AppUser;
import com.sanoli.financedash.domain.DigestFrequency;
import com.sanoli.financedash.domain.Severity;
import com.sanoli.financedash.domain.UserSettings;
import com.sanoli.financedash.radar.engine.MonthProjectionResult;
import com.sanoli.financedash.radar.engine.OverdueReceivablesResult;
import com.sanoli.financedash.radar.engine.RadarEngineService;
import com.sanoli.financedash.radar.engine.SafeToSpendResult;
import com.sanoli.financedash.repository.UserRepository;
import com.sanoli.financedash.repository.UserSettingsRepository;
import com.sanoli.financedash.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RadarDigestServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private RadarEngineService radarEngineService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private EmailService emailService;

    private RadarDigestProperties radarDigestProperties;
    private Clock clock;
    private RadarDigestService radarDigestService;

    @BeforeEach
    void setUp() {
        radarDigestProperties = new RadarDigestProperties();
        radarDigestProperties.setEnabled(true);
        clock = Clock.fixed(Instant.parse("2026-06-17T12:00:00Z"), ZONE);
        radarDigestService = new RadarDigestService(
                radarEngineService,
                userRepository,
                userSettingsRepository,
                emailService,
                radarDigestProperties,
                clock
        );
    }

    @Test
    void shouldTreatWeeklyDigestAsDueAfterSevenDays() {
        UserSettings settings = settingsWithFrequency(DigestFrequency.WEEKLY);
        settings.setLastDigestSentAt(Instant.parse("2026-06-09T12:00:00Z"));

        assertThat(radarDigestService.isDigestDue(settings)).isTrue();
    }

    @Test
    void shouldNotSendDigestWhenFrequencyIsNone() {
        UserSettings settings = settingsWithFrequency(DigestFrequency.NONE);
        AppUser user = user("user@example.com");

        radarDigestService.sendDigestIfDue(user);

        verify(emailService, never()).sendRadarDigest(any(), any(), any());
    }

    @Test
    void shouldSendDigestAndUpdateLastSentAt() {
        UserSettings settings = settingsWithFrequency(DigestFrequency.WEEKLY);
        AppUser user = user("user@example.com");
        when(userSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
        stubEngineResults();

        radarDigestService.sendDigestIfDue(user);

        ArgumentCaptor<UserSettings> savedSettings = ArgumentCaptor.forClass(UserSettings.class);
        verify(emailService).sendRadarDigest(eq("user@example.com"), eq("Radar FinanceDash — resumo da semana"), any());
        verify(userSettingsRepository).save(savedSettings.capture());
        assertThat(savedSettings.getValue().getLastDigestSentAt()).isEqualTo(clock.instant());
    }

    @Test
    void shouldNotifyCriticalAlertByEmail() {
        AppUser user = user("user@example.com");
        Alert alert = new Alert();
        alert.setMessage("Caixa crítico");
        alert.setType(AlertType.MONTH_NEGATIVE);
        alert.setSeverity(Severity.CRITICAL);
        alert.setActionSuggestion("Reduzir gastos variáveis");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        radarDigestService.notifyCriticalAlert(USER_ID, alert);

        verify(emailService).sendCriticalAlert(
                eq("user@example.com"),
                eq("Alerta crítico do Radar FinanceDash"),
                org.mockito.ArgumentMatchers.contains("Caixa crítico")
        );
    }

    private UserSettings settingsWithFrequency(DigestFrequency frequency) {
        UserSettings settings = new UserSettings();
        settings.setUserId(USER_ID);
        settings.setDigestFrequency(frequency);
        return settings;
    }

    private AppUser user(String email) {
        AppUser user = new AppUser();
        user.setId(USER_ID);
        user.setEmail(email);
        return user;
    }

    private void stubEngineResults() {
        when(radarEngineService.projectMonthBalance(USER_ID)).thenReturn(new MonthProjectionResult(
                new BigDecimal("100.00"),
                new BigDecimal("50.00"),
                new BigDecimal("1000.00"),
                true,
                List.of("Premissa A")
        ));
        when(radarEngineService.safeToSpend(USER_ID)).thenReturn(new SafeToSpendResult(
                new BigDecimal("200.00"),
                new BigDecimal("10.00"),
                20,
                new BigDecimal("500.00"),
                List.of("Premissa B")
        ));
        when(radarEngineService.overdueReceivables(USER_ID)).thenReturn(new OverdueReceivablesResult(
                BigDecimal.ZERO,
                List.of(),
                List.of("Premissa C")
        ));
    }
}
