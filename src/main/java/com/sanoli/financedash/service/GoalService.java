package com.sanoli.financedash.service;

import com.sanoli.financedash.domain.Category;
import com.sanoli.financedash.domain.Goal;
import com.sanoli.financedash.domain.GoalType;
import com.sanoli.financedash.domain.Transaction;
import com.sanoli.financedash.domain.TransactionType;
import com.sanoli.financedash.dto.GoalRequest;
import com.sanoli.financedash.dto.GoalResponse;
import com.sanoli.financedash.exception.BusinessException;
import com.sanoli.financedash.exception.ResourceNotFoundException;
import com.sanoli.financedash.repository.CategoryRepository;
import com.sanoli.financedash.repository.GoalRepository;
import com.sanoli.financedash.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class GoalService {

    private static final int MONEY_SCALE = 2;
    private static final int PERCENTAGE_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final GoalRepository goalRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public GoalService(GoalRepository goalRepository, CategoryRepository categoryRepository, TransactionRepository transactionRepository) {
        this.goalRepository = goalRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public GoalResponse create(GoalRequest request) {
        Goal goal = new Goal();
        applyRequest(goal, request);
        return toResponse(goalRepository.save(goal));
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> findAll() {
        return goalRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GoalResponse findById(UUID id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public GoalResponse update(UUID id, GoalRequest request) {
        Goal goal = findEntityById(id);
        applyRequest(goal, request);
        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public void delete(UUID id) {
        goalRepository.delete(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> findByMonthAndYear(Integer month, Integer year) {
        return goalRepository.findByMonthAndYear(month, year)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Goal findEntityById(UUID id) {
        return goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal não encontrada: " + id));
    }

    private void applyRequest(Goal goal, GoalRequest request) {
        Category category = resolveCategory(request.categoryId());
        validateCategoryCompatibility(request.type(), category);

        goal.setTitle(request.title().trim());
        goal.setMonth(request.month());
        goal.setYear(request.year());
        goal.setTargetAmount(request.targetAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        goal.setType(request.type());
        goal.setCategory(category);
    }

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category não encontrada: " + categoryId));

        if (!category.isActive()) {
            throw new ResourceNotFoundException("Category não encontrada: " + categoryId);
        }

        return category;
    }

    private void validateCategoryCompatibility(GoalType goalType, Category category) {
        if (category == null) {
            return;
        }

        if (goalType == GoalType.SAVINGS_TARGET) {
            throw new BusinessException("SAVINGS_TARGET não deve possuir categoryId");
        }

        if (goalType == GoalType.INCOME_TARGET && category.getType() != TransactionType.INCOME) {
            throw new BusinessException("INCOME_TARGET deve usar uma categoria INCOME");
        }

        if (goalType == GoalType.EXPENSE_LIMIT && category.getType() != TransactionType.EXPENSE) {
            throw new BusinessException("EXPENSE_LIMIT deve usar uma categoria EXPENSE");
        }
    }

    private GoalResponse toResponse(Goal goal) {
        BigDecimal currentAmount = calculateCurrentAmount(goal);
        BigDecimal progressPercentage = calculateProgressPercentage(currentAmount, goal.getTargetAmount());
        return GoalResponse.fromEntity(goal, currentAmount, progressPercentage);
    }

    private BigDecimal calculateCurrentAmount(Goal goal) {
        YearMonth yearMonth = YearMonth.of(goal.getYear(), goal.getMonth());
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        List<Transaction> transactions = transactionRepository.findByTransactionDateBetween(startDate, endDate)
                .stream()
                .filter(transaction -> goal.getCategory() == null || transaction.getCategory().getId().equals(goal.getCategory().getId()))
                .toList();

        return switch (goal.getType()) {
            case INCOME_TARGET -> sumByType(transactions, TransactionType.INCOME);
            case EXPENSE_LIMIT -> sumByType(transactions, TransactionType.EXPENSE);
            case SAVINGS_TARGET -> sumByType(transactions, TransactionType.INCOME).subtract(sumByType(transactions, TransactionType.EXPENSE))
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        };
    }

    private BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateProgressPercentage(BigDecimal currentAmount, BigDecimal targetAmount) {
        if (targetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP);
        }

        return currentAmount.multiply(ONE_HUNDRED)
                .divide(targetAmount, PERCENTAGE_SCALE, RoundingMode.HALF_UP);
    }
}

