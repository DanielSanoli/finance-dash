package com.sanoli.financedash.radar.engine;

import com.sanoli.financedash.domain.Transaction;
import com.sanoli.financedash.domain.TransactionStatus;
import com.sanoli.financedash.domain.TransactionType;
import com.sanoli.financedash.domain.UserSettings;
import com.sanoli.financedash.repository.TransactionRepository;
import com.sanoli.financedash.service.UserSettingsService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RadarEngineServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final UUID USER_ID = UUID.randomUUID();

    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final UserSettingsService userSettingsService = mock(UserSettingsService.class);

    private RadarEngineService engineAt(LocalDate today) {
        Clock clock = Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
        when(userSettingsService.getOrCreate(USER_ID)).thenReturn(new UserSettings());
        return new RadarEngineService(transactionRepository, userSettingsService, clock);
    }

    private void mockMonth(List<Transaction> monthTransactions) {
        when(transactionRepository.findByUserIdAndTransactionDateBetween(eq(USER_ID), any(), any()))
                .thenReturn(monthTransactions);
    }

    private void mockUnpaidIncome(List<Transaction> receivables) {
        when(transactionRepository.findByUserIdAndTypeAndStatusIn(eq(USER_ID), eq(TransactionType.INCOME), anyCollection()))
                .thenReturn(receivables);
    }

    private void mockUnpaidExpense(List<Transaction> expenses) {
        when(transactionRepository.findByUserIdAndTypeAndStatusIn(eq(USER_ID), eq(TransactionType.EXPENSE), anyCollection()))
                .thenReturn(expenses);
    }

    @Test
    void projectedBalanceEqualsCurrentWhenNoPendingNorVariables() {
        RadarEngineService engine = engineAt(LocalDate.of(2026, 6, 17));
        Transaction paidIncome = tx(TransactionType.INCOME, "1000.00", TransactionStatus.PAID, LocalDate.of(2026, 6, 5), false);
        mockMonth(List.of(paidIncome));
        mockUnpaidIncome(List.of());
        mockUnpaidExpense(List.of());

        MonthProjectionResult result = engine.projectMonthBalance(USER_ID);

        assertThat(result.currentBalance()).isEqualByComparingTo("1000.00");
        assertThat(result.projectedBalance()).isEqualByComparingTo(result.currentBalance());
        assertThat(result.positive()).isTrue();
        assertThat(result.assumptions()).anyMatch(a -> a.contains("Nenhum recebível pendente"));
    }

    @Test
    void variableProjectionIsZeroOnFirstDayOfMonth() {
        RadarEngineService engine = engineAt(LocalDate.of(2026, 6, 1));
        Transaction variableExpense = tx(TransactionType.EXPENSE, "100.00", TransactionStatus.PAID, LocalDate.of(2026, 6, 1), false);
        mockMonth(List.of(variableExpense));
        mockUnpaidIncome(List.of());
        mockUnpaidExpense(List.of());

        MonthProjectionResult result = engine.projectMonthBalance(USER_ID);

        assertThat(result.currentBalance()).isEqualByComparingTo("-100.00");
        assertThat(result.projectedBalance()).isEqualByComparingTo("-100.00");
        assertThat(result.assumptions()).anyMatch(a -> a.contains("Primeiro dia do mês"));
    }

    @Test
    void projectionListsPendingReceivablesInAssumptions() {
        RadarEngineService engine = engineAt(LocalDate.of(2026, 6, 17));
        Transaction paidIncome = tx(TransactionType.INCOME, "500.00", TransactionStatus.PAID, LocalDate.of(2026, 6, 5), false);
        Transaction pending = tx(TransactionType.INCOME, "800.00", TransactionStatus.PENDING, LocalDate.of(2026, 6, 10), false);
        pending.setDueDate(LocalDate.of(2026, 6, 25));
        pending.setDescription("Projeto X");
        mockMonth(List.of(paidIncome));
        mockUnpaidIncome(List.of(pending));
        mockUnpaidExpense(List.of());

        MonthProjectionResult result = engine.projectMonthBalance(USER_ID);

        assertThat(result.currentBalance()).isEqualByComparingTo("500.00");
        assertThat(result.projectedBalance()).isEqualByComparingTo("1300.00");
        assertThat(result.assumptions()).anyMatch(a -> a.contains("Projeto X"));
    }

    @Test
    void safeToSpendNeverNegativeAndDividesByRemainingDays() {
        RadarEngineService engine = engineAt(LocalDate.of(2026, 6, 17));
        Transaction paidIncome = tx(TransactionType.INCOME, "3000.00", TransactionStatus.PAID, LocalDate.of(2026, 6, 2), false);
        Transaction variablePaid = tx(TransactionType.EXPENSE, "600.00", TransactionStatus.PAID, LocalDate.of(2026, 6, 8), false);
        mockMonth(List.of(paidIncome, variablePaid));
        mockUnpaidIncome(List.of());
        mockUnpaidExpense(List.of());
        UserSettings settings = new UserSettings();
        settings.setMonthlyReserveTarget(new BigDecimal("400.00"));
        when(userSettingsService.getOrCreate(USER_ID)).thenReturn(settings);

        SafeToSpendResult result = engine.safeToSpend(USER_ID);

        // disponivel = 3000 - 0 - 400 - 600 = 2000; dias restantes = 13
        assertThat(result.safeToSpendTotal()).isEqualByComparingTo("2000.00");
        assertThat(result.daysRemaining()).isEqualTo(13);
        assertThat(result.safeToSpendPerDay()).isEqualByComparingTo("153.85");
    }

    @Test
    void safeToSpendFloorsAtZeroWhenOverspent() {
        RadarEngineService engine = engineAt(LocalDate.of(2026, 6, 30));
        Transaction paidIncome = tx(TransactionType.INCOME, "500.00", TransactionStatus.PAID, LocalDate.of(2026, 6, 2), false);
        Transaction variablePaid = tx(TransactionType.EXPENSE, "900.00", TransactionStatus.PAID, LocalDate.of(2026, 6, 8), false);
        mockMonth(List.of(paidIncome, variablePaid));
        mockUnpaidIncome(List.of());
        mockUnpaidExpense(List.of());

        SafeToSpendResult result = engine.safeToSpend(USER_ID);

        assertThat(result.safeToSpendTotal()).isEqualByComparingTo("0.00");
        assertThat(result.safeToSpendPerDay()).isEqualByComparingTo("0.00");
    }

    @Test
    void overdueReceivablesEmptyWhenNone() {
        RadarEngineService engine = engineAt(LocalDate.of(2026, 6, 17));
        mockUnpaidIncome(List.of());

        OverdueReceivablesResult result = engine.overdueReceivables(USER_ID);

        assertThat(result.items()).isEmpty();
        assertThat(result.totalBlocked()).isEqualByComparingTo("0.00");
        assertThat(result.assumptions()).anyMatch(a -> a.contains("Nenhum recebível atrasado"));
    }

    @Test
    void overdueReceivablesSortedByImpactDescending() {
        RadarEngineService engine = engineAt(LocalDate.of(2026, 6, 17));
        UUID clientA = UUID.randomUUID();
        UUID clientB = UUID.randomUUID();

        Transaction small = tx(TransactionType.INCOME, "1000.00", TransactionStatus.PENDING, LocalDate.of(2026, 6, 1), false);
        small.setDueDate(LocalDate.of(2026, 6, 15));
        small.setClientId(clientA);
        small.setClientName("Cliente A");

        Transaction big = tx(TransactionType.INCOME, "500.00", TransactionStatus.PENDING, LocalDate.of(2026, 6, 1), false);
        big.setDueDate(LocalDate.of(2026, 6, 2));
        big.setClientId(clientB);
        big.setClientName("Cliente B");

        mockUnpaidIncome(List.of(small, big));

        OverdueReceivablesResult result = engine.overdueReceivables(USER_ID);

        // small: 1000 * 2 dias = 2000; big: 500 * 15 dias = 7500 -> Cliente B primeiro
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).clientName()).isEqualTo("Cliente B");
        assertThat(result.items().get(0).impact()).isEqualByComparingTo("7500.00");
        assertThat(result.items().get(1).clientName()).isEqualTo("Cliente A");
        assertThat(result.totalBlocked()).isEqualByComparingTo("1500.00");
    }

    @Test
    void needsMoreFreelanceCalculatesGapAndExtraHours() {
        RadarEngineService engine = engineAt(LocalDate.of(2026, 6, 17));
        Transaction paidIncome = tx(TransactionType.INCOME, "2000.00", TransactionStatus.PAID, LocalDate.of(2026, 6, 5), false);
        Transaction pending = tx(TransactionType.INCOME, "500.00", TransactionStatus.PENDING, LocalDate.of(2026, 6, 10), false);
        pending.setDueDate(LocalDate.of(2026, 6, 25));
        mockMonth(List.of(paidIncome));
        mockUnpaidIncome(List.of(pending));
        mockUnpaidExpense(List.of());
        UserSettings settings = new UserSettings();
        settings.setMonthlyIncomeGoal(new BigDecimal("5000.00"));
        settings.setBillableHoursPerMonth(new BigDecimal("100"));
        when(userSettingsService.getOrCreate(USER_ID)).thenReturn(settings);

        FreelanceGapResult result = engine.needsMoreFreelance(USER_ID);

        assertThat(result.needsMoreFreelance()).isTrue();
        assertThat(result.expectedMonthIncome()).isEqualByComparingTo("2500.00");
        assertThat(result.incomeGap()).isEqualByComparingTo("2500.00");
        assertThat(result.referenceHourlyRate()).isEqualByComparingTo("50.00");
        assertThat(result.extraBillableHours()).isEqualByComparingTo("50.00");
    }

    @Test
    void needsMoreFreelanceIsFalseWhenGoalAlreadyMet() {
        RadarEngineService engine = engineAt(LocalDate.of(2026, 6, 17));
        Transaction paidIncome = tx(TransactionType.INCOME, "6000.00", TransactionStatus.PAID, LocalDate.of(2026, 6, 5), false);
        mockMonth(List.of(paidIncome));
        mockUnpaidIncome(List.of());
        mockUnpaidExpense(List.of());
        UserSettings settings = new UserSettings();
        settings.setMonthlyIncomeGoal(new BigDecimal("5000.00"));
        settings.setBillableHoursPerMonth(new BigDecimal("100"));
        when(userSettingsService.getOrCreate(USER_ID)).thenReturn(settings);

        FreelanceGapResult result = engine.needsMoreFreelance(USER_ID);

        assertThat(result.needsMoreFreelance()).isFalse();
        assertThat(result.incomeGap()).isEqualByComparingTo("0.00");
    }

    @Test
    void minimumProjectPriceUsesFixedCostTaxAndMargin() {
        RadarEngineService engine = engineAt(LocalDate.of(2026, 6, 17));
        mockMonth(List.of());
        mockUnpaidIncome(List.of());
        mockUnpaidExpense(List.of());
        UserSettings settings = new UserSettings();
        settings.setMonthlyFixedCost(new BigDecimal("2000.00"));
        settings.setMonthlyIncomeGoal(new BigDecimal("5000.00"));
        settings.setBillableHoursPerMonth(new BigDecimal("100"));
        settings.setTaxRate(new BigDecimal("0.1000"));
        settings.setDesiredMargin(new BigDecimal("0.2000"));
        when(userSettingsService.getOrCreate(USER_ID)).thenReturn(settings);

        MinimumProjectPriceResult result = engine.minimumProjectPrice(USER_ID, new BigDecimal("10"));

        assertThat(result.minimumProjectPrice()).isEqualByComparingTo("1000.00");
        assertThat(result.minimumHourlyRate()).isEqualByComparingTo("100.00");
        assertThat(result.baseHourlyRate()).isEqualByComparingTo("70.00");
    }

    @Test
    void analyzeCutsListsOnlyNonEssentialVariableExpenses() {
        RadarEngineService engine = engineAt(LocalDate.of(2026, 6, 17));
        Transaction essential = tx(TransactionType.EXPENSE, "500.00", TransactionStatus.PAID, LocalDate.of(2026, 6, 8), false);
        essential.setEssential(true);
        Transaction cuttable = tx(TransactionType.EXPENSE, "200.00", TransactionStatus.PAID, LocalDate.of(2026, 6, 9), false);
        cuttable.setEssential(false);
        cuttable.setDescription("Assinatura streaming");
        Transaction paidIncome = tx(TransactionType.INCOME, "1000.00", TransactionStatus.PAID, LocalDate.of(2026, 6, 5), false);
        mockMonth(List.of(paidIncome, essential, cuttable));
        mockUnpaidIncome(List.of());
        mockUnpaidExpense(List.of());

        CutAnalysisResult result = engine.analyzeCuts(USER_ID);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().description()).isEqualTo("Assinatura streaming");
        assertThat(result.totalCuttable()).isEqualByComparingTo("200.00");
        assertThat(result.projectedBalanceAfterCuts())
                .isEqualByComparingTo(result.currentProjectedBalance().add(new BigDecimal("200.00")));
    }

    private Transaction tx(TransactionType type, String amount, TransactionStatus status, LocalDate date, boolean recurring) {
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setDescription(type == TransactionType.INCOME ? "Receita" : "Despesa");
        transaction.setAmount(new BigDecimal(amount));
        transaction.setType(type);
        transaction.setStatus(status);
        transaction.setTransactionDate(date);
        transaction.setDueDate(date);
        transaction.setRecurring(recurring);
        return transaction;
    }
}
