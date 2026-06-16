package com.sanoli.financedash.service;

import com.sanoli.financedash.domain.Transaction;
import com.sanoli.financedash.domain.TransactionType;
import com.sanoli.financedash.dto.CategoryAmountResponse;
import com.sanoli.financedash.dto.MonthlyDashboardResponse;
import com.sanoli.financedash.repository.TransactionRepository;
import com.sanoli.financedash.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final int MONEY_SCALE = 2;
    private static final int PERCENTAGE_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    public DashboardService(TransactionRepository transactionRepository, CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public MonthlyDashboardResponse getMonthlyDashboard(Integer month, Integer year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
                currentUserService.getCurrentUserId(),
                startDate,
                endDate
        );

        BigDecimal totalIncome = sumByType(transactions, TransactionType.INCOME);
        BigDecimal totalExpense = sumByType(transactions, TransactionType.EXPENSE);
        BigDecimal balance = totalIncome.subtract(totalExpense).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        long incomeCount = countByType(transactions, TransactionType.INCOME);
        long expenseCount = countByType(transactions, TransactionType.EXPENSE);

        return new MonthlyDashboardResponse(
                month,
                year,
                startDate,
                endDate,
                totalIncome,
                totalExpense,
                balance,
                transactions.size(),
                incomeCount,
                expenseCount,
                groupByCategory(transactions, TransactionType.EXPENSE, totalExpense),
                groupByCategory(transactions, TransactionType.INCOME, totalIncome)
        );
    }

    private BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private long countByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .count();
    }

    private List<CategoryAmountResponse> groupByCategory(List<Transaction> transactions, TransactionType type, BigDecimal totalByType) {
        Map<CategoryKey, BigDecimal> totalsByCategory = transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .collect(Collectors.groupingBy(
                        transaction -> new CategoryKey(
                                transaction.getCategory().getId(),
                                transaction.getCategory().getName(),
                                transaction.getCategory().getColor()
                        ),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        return totalsByCategory.entrySet()
                .stream()
                .map(entry -> {
                    BigDecimal amount = entry.getValue().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
                    return new CategoryAmountResponse(
                            entry.getKey().id(),
                            entry.getKey().name(),
                            entry.getKey().color(),
                            amount,
                            calculatePercentage(amount, totalByType)
                    );
                })
                .sorted(Comparator.comparing(CategoryAmountResponse::categoryName))
                .toList();
    }

    private BigDecimal calculatePercentage(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP);
        }

        return amount
                .multiply(ONE_HUNDRED)
                .divide(total, PERCENTAGE_SCALE, RoundingMode.HALF_UP);
    }

    private record CategoryKey(java.util.UUID id, String name, String color) {
    }
}

