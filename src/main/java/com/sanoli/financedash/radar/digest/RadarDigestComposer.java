package com.sanoli.financedash.radar.digest;

import com.sanoli.financedash.domain.Alert;
import com.sanoli.financedash.radar.ai.RadarAnswerComposer;
import com.sanoli.financedash.radar.engine.OverdueReceivableItem;

import java.util.stream.Collectors;

/**
 * Monta o texto do digest a partir de números do motor (Regra de Ouro).
 */
public final class RadarDigestComposer {

    private RadarDigestComposer() {
    }

    public static String composeWeeklyDigest(RadarDigestSnapshot snapshot) {
        StringBuilder body = new StringBuilder();
        body.append("⚠️ Radar da semana: no ritmo atual seu mês fecha em R$ ")
                .append(snapshot.monthProjection().projectedBalance().toPlainString())
                .append(". ");

        OverdueReceivableItem topClient = snapshot.topOverdueClient();
        if (topClient != null) {
            body.append(topClient.clientName())
                    .append(" atrasado há ")
                    .append(topClient.daysOverdue())
                    .append(" dias (R$ ")
                    .append(topClient.amount().toPlainString())
                    .append("). ");
        }

        body.append("Safe-to-spend: R$ ")
                .append(snapshot.safeToSpend().safeToSpendPerDay().toPlainString())
                .append("/dia até o dia ")
                .append(snapshot.lastDayOfMonth())
                .append(".");

        body.append("\n\nPremissas: ")
                .append(joinAssumptions(snapshot.assumptions()))
                .append("\n\n")
                .append(RadarAnswerComposer.DISCLAIMER);

        return body.toString();
    }

    public static String composeCriticalAlert(Alert alert) {
        return """
                🚨 Alerta crítico do Radar: %s

                Ação sugerida: %s

                %s""".formatted(
                alert.getMessage(),
                alert.getActionSuggestion() != null ? alert.getActionSuggestion() : "Revise seu caixa hoje",
                RadarAnswerComposer.DISCLAIMER
        );
    }

    private static String joinAssumptions(java.util.List<String> assumptions) {
        if (assumptions == null || assumptions.isEmpty()) {
            return "sem premissas adicionais";
        }
        return assumptions.stream().distinct().collect(Collectors.joining("; "));
    }
}
