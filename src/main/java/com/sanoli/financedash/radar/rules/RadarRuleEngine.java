package com.sanoli.financedash.radar.rules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanoli.financedash.domain.Alert;
import com.sanoli.financedash.domain.AlertType;
import com.sanoli.financedash.domain.Severity;
import com.sanoli.financedash.domain.UserSettings;
import com.sanoli.financedash.radar.digest.RadarDigestService;
import com.sanoli.financedash.radar.engine.MonthProjectionResult;
import com.sanoli.financedash.radar.engine.OverdueReceivableItem;
import com.sanoli.financedash.radar.engine.OverdueReceivablesResult;
import com.sanoli.financedash.radar.engine.RadarEngineService;
import com.sanoli.financedash.repository.AlertRepository;
import com.sanoli.financedash.service.UserSettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Motor de regras do Radar (camada radar.rules). Avalia limiares com base nos
 * números do {@link RadarEngineService} (nunca recalcula valores aqui) e gera
 * {@link Alert}, evitando duplicar alertas do mesmo tipo no mesmo dia.
 */
@Service
public class RadarRuleEngine {

    private final RadarEngineService radarEngineService;
    private final AlertRepository alertRepository;
    private final UserSettingsService userSettingsService;
    private final RadarDigestService radarDigestService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public RadarRuleEngine(
            RadarEngineService radarEngineService,
            AlertRepository alertRepository,
            UserSettingsService userSettingsService,
            RadarDigestService radarDigestService,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.radarEngineService = radarEngineService;
        this.alertRepository = alertRepository;
        this.userSettingsService = userSettingsService;
        this.radarDigestService = radarDigestService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public List<Alert> evaluate(UUID userId) {
        MonthProjectionResult projection = radarEngineService.projectMonthBalance(userId);
        OverdueReceivablesResult overdue = radarEngineService.overdueReceivables(userId);
        UserSettings settings = userSettingsService.getOrCreate(userId);

        List<Alert> created = new ArrayList<>();
        BigDecimal projectedBalance = projection.projectedBalance();

        if (projectedBalance.signum() < 0) {
            persistIfNew(created, userId, AlertType.MONTH_NEGATIVE, Severity.CRITICAL,
                    "No ritmo atual seu mês fecha em R$ " + projectedBalance.toPlainString(),
                    "Cobrar recebíveis atrasados / reduzir gastos variáveis",
                    Map.of("projectedBalance", projectedBalance));
        }

        BigDecimal goal = projection.goal();
        if (goal.signum() > 0 && projectedBalance.compareTo(goal) < 0) {
            BigDecimal missing = goal.subtract(projectedBalance);
            persistIfNew(created, userId, AlertType.BELOW_GOAL, Severity.WARNING,
                    "Faltam R$ " + missing.toPlainString() + " para sua meta do mês",
                    "Acelere recebimentos ou reduza despesas para alcançar a meta",
                    Map.of("goal", goal, "projectedBalance", projectedBalance, "missing", missing));
        }

        Optional<OverdueReceivableItem> worstClient = overdue.items().stream()
                .filter(item -> item.daysOverdue() > 7)
                .findFirst();
        worstClient.ifPresent(item -> {
            String name = item.clientName() != null ? item.clientName() : "cliente";
            persistIfNew(created, userId, AlertType.OVERDUE_CLIENT, Severity.WARNING,
                    String.format("Cobrar %s (R$ %s, %d dias)", name, item.amount().toPlainString(), item.daysOverdue()),
                    "Entre em contato e renegocie o prazo de pagamento",
                    Map.of("clientName", name, "amount", item.amount(), "daysOverdue", item.daysOverdue()));
        });

        BigDecimal reserveTarget = settings.getMonthlyReserveTarget() != null
                ? settings.getMonthlyReserveTarget()
                : BigDecimal.ZERO;
        if (reserveTarget.signum() > 0 && projectedBalance.compareTo(reserveTarget) < 0) {
            persistIfNew(created, userId, AlertType.RESERVE_AT_RISK, Severity.WARNING,
                    "Sua reserva está em risco neste mês",
                    "Evite novos gastos variáveis para preservar a reserva",
                    Map.of("reserveTarget", reserveTarget, "projectedBalance", projectedBalance));
        }

        return created;
    }

    private void persistIfNew(
            List<Alert> created,
            UUID userId,
            AlertType type,
            Severity severity,
            String message,
            String actionSuggestion,
            Map<String, ?> snapshot
    ) {
        Instant startOfDay = LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant();
        if (alertRepository.existsByUserIdAndTypeAndReadFalseAndCreatedAtAfter(userId, type, startOfDay)) {
            return;
        }

        Alert alert = new Alert();
        alert.setUserId(userId);
        alert.setType(type);
        alert.setSeverity(severity);
        alert.setMessage(message);
        alert.setActionSuggestion(actionSuggestion);
        alert.setDataSnapshot(toJson(snapshot));
        Alert saved = alertRepository.save(alert);
        created.add(saved);
        if (severity == Severity.CRITICAL) {
            radarDigestService.notifyCriticalAlert(userId, saved);
        }
    }

    private String toJson(Map<String, ?> snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }
}
