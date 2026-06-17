package com.sanoli.financedash.radar.digest;

import com.sanoli.financedash.domain.Alert;
import com.sanoli.financedash.domain.AlertType;
import com.sanoli.financedash.domain.Severity;
import com.sanoli.financedash.radar.ai.RadarAnswerComposer;
import com.sanoli.financedash.radar.engine.MonthProjectionResult;
import com.sanoli.financedash.radar.engine.OverdueReceivableItem;
import com.sanoli.financedash.radar.engine.OverdueReceivablesResult;
import com.sanoli.financedash.radar.engine.SafeToSpendResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RadarDigestComposerTest {

    @Test
    void shouldComposeWeeklyDigestFromEngineNumbers() {
        UUID clientId = UUID.randomUUID();
        MonthProjectionResult projection = new MonthProjectionResult(
                new BigDecimal("500.00"),
                new BigDecimal("-200.00"),
                new BigDecimal("1000.00"),
                false,
                List.of("Projeção inclui recebíveis pendentes")
        );
        SafeToSpendResult safeToSpend = new SafeToSpendResult(
                new BigDecimal("300.00"),
                new BigDecimal("15.00"),
                20,
                new BigDecimal("1000.00"),
                List.of("Reserva alvo considerada")
        );
        OverdueReceivablesResult overdue = new OverdueReceivablesResult(
                new BigDecimal("800.00"),
                List.of(new OverdueReceivableItem(clientId, "Cliente X", new BigDecimal("800.00"), 12, new BigDecimal("800.00"))),
                List.of("Recebíveis com vencimento anterior a hoje")
        );
        RadarDigestSnapshot snapshot = RadarDigestSnapshot.of(projection, safeToSpend, overdue, 30);

        String body = RadarDigestComposer.composeWeeklyDigest(snapshot);

        assertThat(body).contains("-200.00");
        assertThat(body).contains("Cliente X");
        assertThat(body).contains("12 dias");
        assertThat(body).contains("15.00/dia");
        assertThat(body).contains("Premissas:");
        assertThat(body).contains(RadarAnswerComposer.DISCLAIMER);
    }

    @Test
    void shouldComposeCriticalAlertWithDisclaimer() {
        Alert alert = new Alert();
        alert.setMessage("Saldo projetado negativo");
        alert.setType(AlertType.MONTH_NEGATIVE);
        alert.setSeverity(Severity.CRITICAL);
        alert.setActionSuggestion("Cobrar recebíveis atrasados");

        String body = RadarDigestComposer.composeCriticalAlert(alert);

        assertThat(body).contains("Saldo projetado negativo");
        assertThat(body).contains("Cobrar recebíveis atrasados");
        assertThat(body).contains(RadarAnswerComposer.DISCLAIMER);
    }
}
