package com.sanoli.financedash.radar.ai;

import com.sanoli.financedash.config.RadarAiProperties;
import com.sanoli.financedash.dto.RadarAskResponse;
import com.sanoli.financedash.radar.engine.MonthProjectionResult;
import com.sanoli.financedash.radar.engine.OverdueReceivablesResult;
import com.sanoli.financedash.radar.engine.SafeToSpendResult;
import com.sanoli.financedash.radar.engine.FreelanceGapResult;
import com.sanoli.financedash.radar.engine.MinimumProjectPriceResult;
import com.sanoli.financedash.domain.TransactionStatus;
import com.sanoli.financedash.radar.engine.CutAnalysisItem;
import com.sanoli.financedash.radar.engine.CutAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RadarCopilotServiceTest {

    @Mock
    private RadarEngineTools radarEngineTools;

    @Mock
    private RadarToolInvocationContext invocationContext;

    @Mock
    private ObjectProvider<ChatModel> chatModelProvider;

    @Mock
    private ObjectProvider<org.springframework.ai.chat.client.ChatClient.Builder> chatClientBuilderProvider;

    private RadarAiProperties radarAiProperties;
    private RadarCopilotService radarCopilotService;

    @BeforeEach
    void setUp() {
        radarAiProperties = new RadarAiProperties();
        radarAiProperties.setEnabled(false);
        radarCopilotService = new RadarCopilotService(
                radarEngineTools,
                invocationContext,
                radarAiProperties,
                chatModelProvider,
                chatClientBuilderProvider
        );
    }

    @Test
    void shouldRouteMonthProjectionQuestionDeterministically() {
        MonthProjectionResult projection = new MonthProjectionResult(
                new BigDecimal("1000.00"),
                new BigDecimal("800.00"),
                new BigDecimal("500.00"),
                true,
                List.of("Nenhum recebível pendente até o fim do mês")
        );
        when(radarEngineTools.projetarSaldoMes()).thenReturn(projection);

        RadarAskResponse response = radarCopilotService.ask("Vou fechar o mês positivo?");

        assertThat(response.usedFunction()).isEqualTo("projetarSaldoMes");
        assertThat(response.data()).isEqualTo(projection);
        assertThat(response.answer()).contains("1000.00").contains("800.00");
        assertThat(response.answer()).contains(RadarAnswerComposer.DISCLAIMER);
        verify(invocationContext).clear();
        verify(radarEngineTools).projetarSaldoMes();
    }

    @Test
    void shouldRouteSafeToSpendQuestionDeterministically() {
        SafeToSpendResult safeToSpend = new SafeToSpendResult(
                new BigDecimal("300.00"),
                new BigDecimal("30.00"),
                10,
                new BigDecimal("200.00"),
                List.of("Distribuído em 10 dia(s) restante(s)")
        );
        when(radarEngineTools.safeToSpend()).thenReturn(safeToSpend);

        RadarAskResponse response = radarCopilotService.ask("Quanto posso gastar até o fim do mês?");

        assertThat(response.usedFunction()).isEqualTo("safeToSpend");
        assertThat(response.answer()).contains("300.00").contains("30.00");
    }

    @Test
    void shouldRouteOverdueReceivablesQuestionDeterministically() {
        OverdueReceivablesResult overdue = new OverdueReceivablesResult(
                BigDecimal.ZERO,
                List.of(),
                List.of("Nenhum recebível atrasado na data de hoje")
        );
        when(radarEngineTools.recebiveisAtrasados()).thenReturn(overdue);

        RadarAskResponse response = radarCopilotService.ask("Qual cliente atrasa meu caixa?");

        assertThat(response.usedFunction()).isEqualTo("recebiveisAtrasados");
        assertThat(response.answer()).contains("Não há recebíveis atrasados");
    }

    @Test
    void shouldRouteFreelanceGapQuestionDeterministically() {
        FreelanceGapResult gap = new FreelanceGapResult(
                new BigDecimal("5000.00"),
                new BigDecimal("2500.00"),
                new BigDecimal("2500.00"),
                new BigDecimal("50.00"),
                new BigDecimal("50.00"),
                true,
                List.of("Meta de receita mensal: R$ 5000.00")
        );
        when(radarEngineTools.precisaMaisFreela()).thenReturn(gap);

        RadarAskResponse response = radarCopilotService.ask("Preciso pegar mais freelas este mês?");

        assertThat(response.usedFunction()).isEqualTo("precisaMaisFreela");
        assertThat(response.answer()).contains("2500.00");
    }

    @Test
    void shouldRouteMinimumProjectPriceWhenHoursArePresent() {
        MinimumProjectPriceResult price = new MinimumProjectPriceResult(
                new BigDecimal("40.00"),
                new BigDecimal("4000.00"),
                new BigDecimal("100.00"),
                new BigDecimal("70.00"),
                new BigDecimal("0.1000"),
                new BigDecimal("0.2000"),
                List.of("Preço mínimo calculado")
        );
        when(radarEngineTools.precoMinimoProjeto(40.0)).thenReturn(price);

        RadarAskResponse response = radarCopilotService.ask("Qual preço mínimo para 40 horas?");

        assertThat(response.usedFunction()).isEqualTo("precoMinimoProjeto");
        assertThat(response.answer()).contains("4000.00");
    }

    @Test
    void shouldRouteCutAnalysisQuestionDeterministically() {
        CutAnalysisResult cuts = new CutAnalysisResult(
                new BigDecimal("200.00"),
                new BigDecimal("-100.00"),
                new BigDecimal("100.00"),
                List.of(new CutAnalysisItem(
                        UUID.randomUUID(),
                        "Streaming",
                        "Lazer",
                        new BigDecimal("200.00"),
                        TransactionStatus.PAID
                )),
                List.of("What-if simulado")
        );
        when(radarEngineTools.analisarCortes()).thenReturn(cuts);

        RadarAskResponse response = radarCopilotService.ask("O que posso cortar para economizar?");

        assertThat(response.usedFunction()).isEqualTo("analisarCortes");
        assertThat(response.answer()).contains("200.00");
    }

    @Test
    void shouldListSupportedQuestionsWhenIntentIsUnknown() {
        RadarAskResponse response = radarCopilotService.ask("Como está o clima hoje?");

        assertThat(response.usedFunction()).isEqualTo("unsupported");
        assertThat(response.data()).isNull();
        assertThat(response.answer()).contains("Não identifiquei uma pergunta suportada");
        assertThat(response.answer()).doesNotContain("R$");
    }
}
