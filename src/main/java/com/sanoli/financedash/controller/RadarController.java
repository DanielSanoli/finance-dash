package com.sanoli.financedash.controller;

import com.sanoli.financedash.dto.AlertResponse;
import com.sanoli.financedash.dto.RadarAskRequest;
import com.sanoli.financedash.dto.RadarAskResponse;
import com.sanoli.financedash.radar.ai.RadarCopilotService;
import com.sanoli.financedash.radar.engine.MonthProjectionResult;
import com.sanoli.financedash.radar.engine.OverdueReceivablesResult;
import com.sanoli.financedash.radar.engine.RadarEngineService;
import com.sanoli.financedash.radar.engine.SafeToSpendResult;
import com.sanoli.financedash.radar.engine.FreelanceGapResult;
import com.sanoli.financedash.radar.engine.MinimumProjectPriceResult;
import com.sanoli.financedash.radar.engine.CutAnalysisResult;
import com.sanoli.financedash.security.CurrentUserService;
import com.sanoli.financedash.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/radar")
public class RadarController {

    private final RadarEngineService radarEngineService;
    private final CurrentUserService currentUserService;
    private final AlertService alertService;
    private final RadarCopilotService radarCopilotService;

    public RadarController(
            RadarEngineService radarEngineService,
            CurrentUserService currentUserService,
            AlertService alertService,
            RadarCopilotService radarCopilotService
    ) {
        this.radarEngineService = radarEngineService;
        this.currentUserService = currentUserService;
        this.alertService = alertService;
        this.radarCopilotService = radarCopilotService;
    }

    @GetMapping("/month-projection")
    @Operation(summary = "Projeta o saldo do mês corrente")
    public ResponseEntity<MonthProjectionResult> monthProjection() {
        return ResponseEntity.ok(radarEngineService.projectMonthBalance(currentUserService.getCurrentUserId()));
    }

    @GetMapping("/safe-to-spend")
    @Operation(summary = "Calcula quanto é seguro gastar até o fim do mês")
    public ResponseEntity<SafeToSpendResult> safeToSpend() {
        return ResponseEntity.ok(radarEngineService.safeToSpend(currentUserService.getCurrentUserId()));
    }

    @GetMapping("/overdue-receivables")
    @Operation(summary = "Lista recebíveis atrasados e o caixa travado")
    public ResponseEntity<OverdueReceivablesResult> overdueReceivables() {
        return ResponseEntity.ok(radarEngineService.overdueReceivables(currentUserService.getCurrentUserId()));
    }

    @GetMapping("/freelance-gap")
    @Operation(summary = "Calcula gap de receita e horas extras para bater a meta do mês")
    public ResponseEntity<FreelanceGapResult> freelanceGap() {
        return ResponseEntity.ok(radarEngineService.needsMoreFreelance(currentUserService.getCurrentUserId()));
    }

    @GetMapping("/minimum-project-price")
    @Operation(summary = "Calcula preço mínimo sugerido para um projeto")
    public ResponseEntity<MinimumProjectPriceResult> minimumProjectPrice(
            @RequestParam(name = "estimatedHours") BigDecimal estimatedHours
    ) {
        return ResponseEntity.ok(radarEngineService.minimumProjectPrice(
                currentUserService.getCurrentUserId(),
                estimatedHours
        ));
    }

    @GetMapping("/cut-analysis")
    @Operation(summary = "Lista despesas não essenciais e simula impacto no saldo projetado")
    public ResponseEntity<CutAnalysisResult> cutAnalysis() {
        return ResponseEntity.ok(radarEngineService.analyzeCuts(currentUserService.getCurrentUserId()));
    }

    @PostMapping("/ask")
    @Operation(summary = "Pergunta ao copiloto financeiro do Radar")
    public ResponseEntity<RadarAskResponse> ask(@Valid @RequestBody RadarAskRequest request) {
        return ResponseEntity.ok(radarCopilotService.ask(request.question()));
    }

    @GetMapping("/alerts")
    @Operation(summary = "Lista os alertas do Radar do usuário")
    public ResponseEntity<List<AlertResponse>> alerts(
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly
    ) {
        return ResponseEntity.ok(alertService.listForCurrentUser(unreadOnly));
    }

    @PostMapping("/alerts/{id}/read")
    @Operation(summary = "Marca um alerta como lido")
    public ResponseEntity<AlertResponse> markAlertRead(@PathVariable UUID id) {
        return ResponseEntity.ok(alertService.markAsRead(id));
    }
}
