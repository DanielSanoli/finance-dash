package com.sanoli.financedash.radar.engine;

import com.sanoli.financedash.domain.Transaction;
import com.sanoli.financedash.domain.TransactionStatus;
import com.sanoli.financedash.domain.TransactionType;
import com.sanoli.financedash.domain.UserSettings;
import com.sanoli.financedash.repository.TransactionRepository;
import com.sanoli.financedash.service.UserSettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Motor determinístico do Radar (camada radar.engine).
 *
 * Regra de Ouro: todos os valores monetários são calculados aqui, em BigDecimal
 * (HALF_EVEN, 2 casas). A camada de IA apenas interpreta e formata estes DTOs.
 */
@Service
public class RadarEngineService {

    private static final int MONEY_SCALE = 2;
    private static final List<TransactionStatus> UNPAID = List.of(TransactionStatus.PENDING, TransactionStatus.OVERDUE);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TransactionRepository transactionRepository;
    private final UserSettingsService userSettingsService;
    private final Clock clock;

    public RadarEngineService(
            TransactionRepository transactionRepository,
            UserSettingsService userSettingsService,
            Clock clock
    ) {
        this.transactionRepository = transactionRepository;
        this.userSettingsService = userSettingsService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MonthProjectionResult projectMonthBalance(UUID userId) {
        Context ctx = loadContext(userId);
        List<String> assumptions = new ArrayList<>();

        BigDecimal currentBalance = ctx.paidIncome.subtract(ctx.paidExpense);

        BigDecimal expectedReceivables = BigDecimal.ZERO;
        for (Transaction receivable : ctx.expectedReceivables) {
            expectedReceivables = expectedReceivables.add(receivable.getAmount());
            assumptions.add(String.format(
                    "Recebível pendente considerado: %s R$ %s (vence %s)",
                    receivable.getDescription(),
                    money(receivable.getAmount()).toPlainString(),
                    formatDue(receivable)
            ));
        }

        BigDecimal variableProjection;
        if (ctx.daysElapsed > 0) {
            variableProjection = ctx.variableExpensesSoFar
                    .divide(BigDecimal.valueOf(ctx.daysElapsed), 10, RoundingMode.HALF_EVEN)
                    .multiply(BigDecimal.valueOf(ctx.daysRemaining));
            assumptions.add(String.format(
                    "Projeção de gastos variáveis: R$ %s com base em %d dia(s) decorrido(s)",
                    money(variableProjection).toPlainString(),
                    ctx.daysElapsed
            ));
        } else {
            variableProjection = BigDecimal.ZERO;
            assumptions.add("Primeiro dia do mês: sem histórico para projetar gastos variáveis");
        }

        BigDecimal projectedExpensesRest = ctx.unpaidRecurringExpenses.add(variableProjection);
        BigDecimal projectedBalance = currentBalance.add(expectedReceivables).subtract(projectedExpensesRest);
        BigDecimal goal = nullToZero(ctx.settings.getMonthlyIncomeGoal());

        if (ctx.expectedReceivables.isEmpty()) {
            assumptions.add("Nenhum recebível pendente até o fim do mês");
        }

        return new MonthProjectionResult(
                money(currentBalance),
                money(projectedBalance),
                money(goal),
                money(projectedBalance).signum() >= 0,
                assumptions
        );
    }

    @Transactional(readOnly = true)
    public SafeToSpendResult safeToSpend(UUID userId) {
        Context ctx = loadContext(userId);
        List<String> assumptions = new ArrayList<>();

        BigDecimal expectedReceivables = BigDecimal.ZERO;
        for (Transaction receivable : ctx.expectedReceivables) {
            expectedReceivables = expectedReceivables.add(receivable.getAmount());
        }

        BigDecimal expectedMonthIncome = ctx.paidIncome.add(expectedReceivables);
        BigDecimal reserveTarget = nullToZero(ctx.settings.getMonthlyReserveTarget());

        BigDecimal available = expectedMonthIncome
                .subtract(ctx.unpaidRecurringExpenses)
                .subtract(reserveTarget)
                .subtract(ctx.variableExpensesPaid);

        BigDecimal safeToSpendTotal = available.max(BigDecimal.ZERO);
        int divisor = Math.max(1, ctx.daysRemaining);
        BigDecimal safeToSpendPerDay = safeToSpendTotal.divide(BigDecimal.valueOf(divisor), MONEY_SCALE, RoundingMode.HALF_EVEN);

        assumptions.add(String.format("Receita prevista no mês: R$ %s", money(expectedMonthIncome).toPlainString()));
        assumptions.add(String.format("Despesas fixas restantes: R$ %s", money(ctx.unpaidRecurringExpenses).toPlainString()));
        assumptions.add(String.format("Reserva preservada: R$ %s", money(reserveTarget).toPlainString()));
        assumptions.add(String.format("Gastos variáveis já feitos: R$ %s", money(ctx.variableExpensesPaid).toPlainString()));
        assumptions.add(String.format("Distribuído em %d dia(s) restante(s)", divisor));
        if (available.signum() < 0) {
            assumptions.add("Disponível negativo: safe-to-spend ajustado para R$ 0,00");
        }

        return new SafeToSpendResult(
                money(safeToSpendTotal),
                safeToSpendPerDay,
                ctx.daysRemaining,
                money(reserveTarget),
                assumptions
        );
    }

    @Transactional(readOnly = true)
    public OverdueReceivablesResult overdueReceivables(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        List<Transaction> receivables = transactionRepository
                .findByUserIdAndTypeAndStatusIn(userId, TransactionType.INCOME, UNPAID);

        Map<String, Aggregate> grouped = new LinkedHashMap<>();
        BigDecimal totalBlocked = BigDecimal.ZERO;

        for (Transaction receivable : receivables) {
            LocalDate dueDate = receivable.getDueDate();
            if (dueDate == null || !dueDate.isBefore(today)) {
                continue;
            }

            long daysOverdue = ChronoUnit.DAYS.between(dueDate, today);
            BigDecimal impact = receivable.getAmount().multiply(BigDecimal.valueOf(daysOverdue));
            totalBlocked = totalBlocked.add(receivable.getAmount());

            String key = receivable.getClientId() != null
                    ? "client:" + receivable.getClientId()
                    : "tx:" + receivable.getId();

            Aggregate aggregate = grouped.computeIfAbsent(key, ignored -> new Aggregate(
                    receivable.getClientId(),
                    receivable.getClientId() != null ? receivable.getClientName() : receivable.getDescription()
            ));
            aggregate.amount = aggregate.amount.add(receivable.getAmount());
            aggregate.impact = aggregate.impact.add(impact);
            aggregate.daysOverdue = Math.max(aggregate.daysOverdue, daysOverdue);
        }

        List<OverdueReceivableItem> items = new ArrayList<>();
        for (Aggregate aggregate : grouped.values()) {
            items.add(new OverdueReceivableItem(
                    aggregate.clientId,
                    aggregate.label,
                    money(aggregate.amount),
                    aggregate.daysOverdue,
                    money(aggregate.impact)
            ));
        }
        items.sort(Comparator.comparing(OverdueReceivableItem::impact).reversed());

        List<String> assumptions = new ArrayList<>();
        if (items.isEmpty()) {
            assumptions.add("Nenhum recebível atrasado na data de hoje");
        } else {
            assumptions.add(String.format("%d recebível(is) atrasado(s) considerando vencimento < hoje", items.size()));
            assumptions.add("Impacto = valor × dias de atraso, ordenado do maior para o menor");
        }

        return new OverdueReceivablesResult(money(totalBlocked), items, assumptions);
    }

    private Context loadContext(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        YearMonth yearMonth = YearMonth.from(today);
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();

        long daysElapsed = ChronoUnit.DAYS.between(firstDay, today);
        int daysRemaining = (int) ChronoUnit.DAYS.between(today, lastDay);

        List<Transaction> monthTransactions = transactionRepository
                .findByUserIdAndTransactionDateBetween(userId, firstDay, lastDay);

        BigDecimal paidIncome = BigDecimal.ZERO;
        BigDecimal paidExpense = BigDecimal.ZERO;
        BigDecimal variableExpensesPaid = BigDecimal.ZERO;
        BigDecimal variableExpensesSoFar = BigDecimal.ZERO;

        for (Transaction transaction : monthTransactions) {
            if (transaction.getStatus() != TransactionStatus.PAID) {
                continue;
            }
            if (transaction.getType() == TransactionType.INCOME) {
                paidIncome = paidIncome.add(transaction.getAmount());
            } else if (transaction.getType() == TransactionType.EXPENSE) {
                paidExpense = paidExpense.add(transaction.getAmount());
                if (!transaction.isRecurring()) {
                    variableExpensesPaid = variableExpensesPaid.add(transaction.getAmount());
                    if (!transaction.getTransactionDate().isAfter(today)) {
                        variableExpensesSoFar = variableExpensesSoFar.add(transaction.getAmount());
                    }
                }
            }
        }

        List<Transaction> expectedReceivables = new ArrayList<>();
        for (Transaction receivable : transactionRepository.findByUserIdAndTypeAndStatusIn(userId, TransactionType.INCOME, UNPAID)) {
            if (receivable.getDueDate() != null && !receivable.getDueDate().isAfter(lastDay)) {
                expectedReceivables.add(receivable);
            }
        }

        BigDecimal unpaidRecurringExpenses = BigDecimal.ZERO;
        for (Transaction expense : transactionRepository.findByUserIdAndTypeAndStatusIn(userId, TransactionType.EXPENSE, UNPAID)) {
            if (expense.isRecurring() && expense.getDueDate() != null && !expense.getDueDate().isAfter(lastDay)) {
                unpaidRecurringExpenses = unpaidRecurringExpenses.add(expense.getAmount());
            }
        }

        UserSettings settings = userSettingsService.getOrCreate(userId);

        Context context = new Context();
        context.today = today;
        context.lastDay = lastDay;
        context.daysElapsed = daysElapsed;
        context.daysRemaining = daysRemaining;
        context.paidIncome = paidIncome;
        context.paidExpense = paidExpense;
        context.variableExpensesPaid = variableExpensesPaid;
        context.variableExpensesSoFar = variableExpensesSoFar;
        context.expectedReceivables = expectedReceivables;
        context.unpaidRecurringExpenses = unpaidRecurringExpenses;
        context.settings = settings;
        return context;
    }

    private String formatDue(Transaction transaction) {
        return transaction.getDueDate() != null ? transaction.getDueDate().format(DATE_FORMAT) : "sem data";
    }

    private BigDecimal money(BigDecimal value) {
        return nullToZero(value).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static final class Context {
        LocalDate today;
        LocalDate lastDay;
        long daysElapsed;
        int daysRemaining;
        BigDecimal paidIncome;
        BigDecimal paidExpense;
        BigDecimal variableExpensesPaid;
        BigDecimal variableExpensesSoFar;
        List<Transaction> expectedReceivables;
        BigDecimal unpaidRecurringExpenses;
        UserSettings settings;
    }

    private static final class Aggregate {
        private final UUID clientId;
        private final String label;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal impact = BigDecimal.ZERO;
        private long daysOverdue = 0;

        private Aggregate(UUID clientId, String label) {
            this.clientId = clientId;
            this.label = label;
        }
    }
}
