package com.sanoli.financedash.radar.digest;

import com.sanoli.financedash.radar.engine.MonthProjectionResult;
import com.sanoli.financedash.radar.engine.OverdueReceivableItem;
import com.sanoli.financedash.radar.engine.OverdueReceivablesResult;
import com.sanoli.financedash.radar.engine.SafeToSpendResult;

import java.util.List;

public record RadarDigestSnapshot(
        MonthProjectionResult monthProjection,
        SafeToSpendResult safeToSpend,
        OverdueReceivablesResult overdueReceivables,
        int lastDayOfMonth,
        List<String> assumptions
) {
    public static RadarDigestSnapshot of(
            MonthProjectionResult monthProjection,
            SafeToSpendResult safeToSpend,
            OverdueReceivablesResult overdueReceivables,
            int lastDayOfMonth
    ) {
        List<String> assumptions = new java.util.ArrayList<>();
        assumptions.addAll(monthProjection.assumptions());
        assumptions.addAll(safeToSpend.assumptions());
        assumptions.addAll(overdueReceivables.assumptions());
        return new RadarDigestSnapshot(monthProjection, safeToSpend, overdueReceivables, lastDayOfMonth, assumptions);
    }

    public OverdueReceivableItem topOverdueClient() {
        if (overdueReceivables.items().isEmpty()) {
            return null;
        }
        return overdueReceivables.items().getFirst();
    }
}
