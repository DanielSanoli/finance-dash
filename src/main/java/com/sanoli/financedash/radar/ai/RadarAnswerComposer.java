package com.sanoli.financedash.radar.ai;

import com.sanoli.financedash.radar.engine.MonthProjectionResult;
import com.sanoli.financedash.radar.engine.OverdueReceivableItem;
import com.sanoli.financedash.radar.engine.OverdueReceivablesResult;
import com.sanoli.financedash.radar.engine.SafeToSpendResult;
import com.sanoli.financedash.radar.engine.FreelanceGapResult;
import com.sanoli.financedash.radar.engine.MinimumProjectPriceResult;
import com.sanoli.financedash.radar.engine.CutAnalysisResult;
import com.sanoli.financedash.radar.engine.CutAnalysisItem;

import java.util.List;
import java.util.stream.Collectors;

public final class RadarAnswerComposer {

    public static final String DISCLAIMER = "Sugestão, não consultoria financeira.";

    public static final String UNSUPPORTED_MESSAGE = """
            Não identifiquei uma pergunta suportada. Posso ajudar com:
            - Vou fechar o mês positivo?
            - Quanto posso gastar até o fim do mês?
            - Quais recebíveis ou clientes estão atrasando meu caixa?
            - Preciso pegar mais freelas este mês?
            - Qual o preço mínimo para um projeto de X horas?
            - O que posso cortar para melhorar meu caixa?

            """ + DISCLAIMER;

    private RadarAnswerComposer() {
    }

    public static String composeMonthProjection(MonthProjectionResult result) {
        String action = result.positive()
                ? "Mantenha o ritmo de recebimentos e evite aumentar gastos variáveis desnecessários."
                : "Priorize cobrar recebíveis pendentes e reduza gastos variáveis até regularizar o mês.";
        return """
                Seu saldo atual é R$ %s e a projeção de fechamento do mês é R$ %s (meta: R$ %s).

                Ação sugerida: %s
                Premissas: %s

                %s""".formatted(
                result.currentBalance().toPlainString(),
                result.projectedBalance().toPlainString(),
                result.goal().toPlainString(),
                action,
                joinAssumptions(result.assumptions()),
                DISCLAIMER
        );
    }

    public static String composeSafeToSpend(SafeToSpendResult result) {
        return """
                Você pode gastar com segurança até R$ %s no restante do mês (cerca de R$ %s por dia em %d dia(s)).

                Ação sugerida: use esse teto como referência diária e revise se surgirem receitas ou despesas extras.
                Premissas: %s

                %s""".formatted(
                result.safeToSpendTotal().toPlainString(),
                result.safeToSpendPerDay().toPlainString(),
                result.daysRemaining(),
                joinAssumptions(result.assumptions()),
                DISCLAIMER
        );
    }

    public static String composeOverdueReceivables(OverdueReceivablesResult result) {
        if (result.items().isEmpty()) {
            return """
                    Não há recebíveis atrasados travando seu caixa neste momento.

                    Ação sugerida: mantenha o acompanhamento semanal dos vencimentos pendentes.
                    Premissas: %s

                    %s""".formatted(joinAssumptions(result.assumptions()), DISCLAIMER);
        }

        String clients = result.items().stream()
                .limit(3)
                .map(RadarAnswerComposer::formatOverdueItem)
                .collect(Collectors.joining("; "));

        return """
                Há R$ %s travados em recebíveis atrasados. Principais casos: %s.

                Ação sugerida: cobre os clientes com maior impacto primeiro e renegocie prazos se necessário.
                Premissas: %s

                %s""".formatted(
                result.totalBlocked().toPlainString(),
                clients,
                joinAssumptions(result.assumptions()),
                DISCLAIMER
        );
    }

    public static String composeFreelanceGap(FreelanceGapResult result) {
        if (!result.needsMoreFreelance()) {
            return """
                    Sua receita prevista de R$ %s já cobre a meta mensal de R$ %s.

                    Ação sugerida: mantenha o pipeline comercial e proteja margem nos próximos projetos.
                    Premissas: %s

                    %s""".formatted(
                    result.expectedMonthIncome().toPlainString(),
                    result.monthlyIncomeGoal().toPlainString(),
                    joinAssumptions(result.assumptions()),
                    DISCLAIMER
            );
        }

        String hoursPart = result.extraBillableHours().signum() > 0
                ? " (~" + result.extraBillableHours().toPlainString() + " h extras)"
                : "";

        return """
                Falta R$ %s de receita para bater a meta de R$ %s%s.

                Ação sugerida: prospecte novos projetos ou antecipe cobranças de recebíveis pendentes.
                Premissas: %s

                %s""".formatted(
                result.incomeGap().toPlainString(),
                result.monthlyIncomeGoal().toPlainString(),
                hoursPart,
                joinAssumptions(result.assumptions()),
                DISCLAIMER
        );
    }

    public static String composeMinimumProjectPrice(MinimumProjectPriceResult result) {
        return """
                Para %s h, o preço mínimo sugerido é R$ %s (R$ %s/h).

                Ação sugerida: negocie acima desse piso para cobrir imprevistos e manter margem.
                Premissas: %s

                %s""".formatted(
                result.estimatedHours().toPlainString(),
                result.minimumProjectPrice().toPlainString(),
                result.minimumHourlyRate().toPlainString(),
                joinAssumptions(result.assumptions()),
                DISCLAIMER
        );
    }

    public static String composeCutAnalysis(CutAnalysisResult result) {
        if (result.items().isEmpty()) {
            return """
                    Não encontrei despesas variáveis marcadas como não essenciais neste mês.

                    Ação sugerida: marque despesas dispensáveis com essential=false para o Radar sugerir cortes.
                    Premissas: %s

                    %s""".formatted(joinAssumptions(result.assumptions()), DISCLAIMER);
        }

        String topCuts = result.items().stream()
                .limit(3)
                .map(CutAnalysisItem::description)
                .collect(Collectors.joining("; "));

        return """
                Há R$ %s em gastos não essenciais. Cortando tudo, o saldo projetado passaria de R$ %s para R$ %s.
                Principais itens: %s.

                Ação sugerida: corte primeiro os maiores gastos dispensáveis e revise assinaturas recorrentes.
                Premissas: %s

                %s""".formatted(
                result.totalCuttable().toPlainString(),
                result.currentProjectedBalance().toPlainString(),
                result.projectedBalanceAfterCuts().toPlainString(),
                topCuts,
                joinAssumptions(result.assumptions()),
                DISCLAIMER
        );
    }

    public static String appendGuardrails(String answer, List<String> assumptions) {
        if (answer == null || answer.isBlank()) {
            return UNSUPPORTED_MESSAGE;
        }
        if (answer.contains(DISCLAIMER)) {
            return answer;
        }
        return answer.strip() + "\n\nPremissas: " + joinAssumptions(assumptions) + "\n\n" + DISCLAIMER;
    }

    private static String formatOverdueItem(OverdueReceivableItem item) {
        return item.clientName() + " (R$ " + item.amount().toPlainString() + ", " + item.daysOverdue() + " dias)";
    }

    private static String joinAssumptions(List<String> assumptions) {
        if (assumptions == null || assumptions.isEmpty()) {
            return "sem premissas adicionais";
        }
        return String.join("; ", assumptions);
    }
}
