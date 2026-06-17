package com.sanoli.financedash.radar.ai;

import com.sanoli.financedash.radar.engine.MonthProjectionResult;
import com.sanoli.financedash.radar.engine.OverdueReceivablesResult;
import com.sanoli.financedash.radar.engine.RadarEngineService;
import com.sanoli.financedash.radar.engine.SafeToSpendResult;
import com.sanoli.financedash.radar.engine.FreelanceGapResult;
import com.sanoli.financedash.radar.engine.MinimumProjectPriceResult;
import com.sanoli.financedash.radar.engine.CutAnalysisResult;
import com.sanoli.financedash.security.CurrentUserService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Ferramentas expostas ao copiloto (function calling). Todo valor monetário vem do motor.
 */
@Component
public class RadarEngineTools {

    private final RadarEngineService radarEngineService;
    private final CurrentUserService currentUserService;
    private final RadarToolInvocationContext invocationContext;

    public RadarEngineTools(
            RadarEngineService radarEngineService,
            CurrentUserService currentUserService,
            RadarToolInvocationContext invocationContext
    ) {
        this.radarEngineService = radarEngineService;
        this.currentUserService = currentUserService;
        this.invocationContext = invocationContext;
    }

    @Tool(description = "Projeta se o mês fecha positivo. Retorna saldo atual, saldo projetado, meta e premissas.")
    public MonthProjectionResult projetarSaldoMes() {
        MonthProjectionResult result = radarEngineService.projectMonthBalance(currentUserService.getCurrentUserId());
        invocationContext.record("projetarSaldoMes", result);
        return result;
    }

    @Tool(description = "Calcula quanto é seguro gastar até o fim do mês (total e por dia), com premissas.")
    public SafeToSpendResult safeToSpend() {
        SafeToSpendResult result = radarEngineService.safeToSpend(currentUserService.getCurrentUserId());
        invocationContext.record("safeToSpend", result);
        return result;
    }

    @Tool(description = "Lista recebíveis atrasados, clientes impactados e total de caixa travado.")
    public OverdueReceivablesResult recebiveisAtrasados() {
        OverdueReceivablesResult result = radarEngineService.overdueReceivables(currentUserService.getCurrentUserId());
        invocationContext.record("recebiveisAtrasados", result);
        return result;
    }

    @Tool(description = "Calcula se falta receita/freela para bater a meta mensal e quantas horas extras seriam necessárias.")
    public FreelanceGapResult precisaMaisFreela() {
        FreelanceGapResult result = radarEngineService.needsMoreFreelance(currentUserService.getCurrentUserId());
        invocationContext.record("precisaMaisFreela", result);
        return result;
    }

    @Tool(description = "Calcula preço mínimo sugerido para um projeto com base em horas estimadas, custos fixos, impostos e margem.")
    public MinimumProjectPriceResult precoMinimoProjeto(
            @ToolParam(description = "Horas estimadas do projeto") double estimatedHours
    ) {
        MinimumProjectPriceResult result = radarEngineService.minimumProjectPrice(
                currentUserService.getCurrentUserId(),
                BigDecimal.valueOf(estimatedHours)
        );
        invocationContext.record("precoMinimoProjeto", result);
        return result;
    }

    @Tool(description = "Lista despesas variáveis não essenciais do mês e simula quanto o saldo projetado melhoraria ao cortá-las.")
    public CutAnalysisResult analisarCortes() {
        CutAnalysisResult result = radarEngineService.analyzeCuts(currentUserService.getCurrentUserId());
        invocationContext.record("analisarCortes", result);
        return result;
    }
}
