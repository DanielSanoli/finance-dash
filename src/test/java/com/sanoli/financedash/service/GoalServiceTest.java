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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private GoalService goalService;

    @Test
    void shouldCreateIncomeTargetGoalWithCalculatedProgress() {
        Category category = category("Servicos", TransactionType.INCOME);
        GoalRequest request = new GoalRequest("Meta de receita", 7, 2026, new BigDecimal("5000.00"), GoalType.INCOME_TARGET, category.getId());
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> {
            Goal goal = invocation.getArgument(0);
            goal.setId(UUID.randomUUID());
            goal.setCreatedAt(LocalDateTime.of(2026, 7, 1, 10, 0));
            return goal;
        });
        when(transactionRepository.findByTransactionDateBetween(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(List.of(
                        transaction("2500.00", TransactionType.INCOME, category),
                        transaction("1000.00", TransactionType.INCOME, category)
                ));

        GoalResponse response = goalService.create(request);

        assertThat(response.title()).isEqualTo("Meta de receita");
        assertThat(response.currentAmount()).isEqualByComparingTo("3500.00");
        assertThat(response.progressPercentage()).isEqualByComparingTo("70.00");
        assertThat(response.categoryName()).isEqualTo("Servicos");

        ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
        verify(goalRepository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo(category);
    }

    @Test
    void shouldCalculateExpenseLimitProgress() {
        Category category = category("Software", TransactionType.EXPENSE);
        Goal goal = goal("Limite software", GoalType.EXPENSE_LIMIT, new BigDecimal("1000.00"), category);
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));
        when(transactionRepository.findByTransactionDateBetween(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(List.of(
                        transaction("250.00", TransactionType.EXPENSE, category),
                        transaction("300.00", TransactionType.EXPENSE, category)
                ));

        GoalResponse response = goalService.findById(goal.getId());

        assertThat(response.currentAmount()).isEqualByComparingTo("550.00");
        assertThat(response.progressPercentage()).isEqualByComparingTo("55.00");
    }

    @Test
    void shouldCalculateSavingsTargetProgress() {
        Category incomeCategory = category("Salario", TransactionType.INCOME);
        Category expenseCategory = category("Alimentacao", TransactionType.EXPENSE);
        Goal goal = goal("Economizar", GoalType.SAVINGS_TARGET, new BigDecimal("2000.00"), null);
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));
        when(transactionRepository.findByTransactionDateBetween(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(List.of(
                        transaction("5000.00", TransactionType.INCOME, incomeCategory),
                        transaction("1500.00", TransactionType.EXPENSE, expenseCategory)
                ));

        GoalResponse response = goalService.findById(goal.getId());

        assertThat(response.currentAmount()).isEqualByComparingTo("3500.00");
        assertThat(response.progressPercentage()).isEqualByComparingTo("175.00");
    }

    @Test
    void shouldRejectSavingsTargetWithCategory() {
        Category category = category("Salario", TransactionType.INCOME);
        GoalRequest request = new GoalRequest("Economizar", 7, 2026, new BigDecimal("2000.00"), GoalType.SAVINGS_TARGET, category.getId());
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> goalService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SAVINGS_TARGET");
    }

    @Test
    void shouldThrowWhenGoalDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(goalRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> goalService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Goal não encontrada");
    }

    private Goal goal(String title, GoalType type, BigDecimal targetAmount, Category category) {
        Goal goal = new Goal();
        goal.setId(UUID.randomUUID());
        goal.setTitle(title);
        goal.setMonth(7);
        goal.setYear(2026);
        goal.setTargetAmount(targetAmount);
        goal.setType(type);
        goal.setCategory(category);
        goal.setCreatedAt(LocalDateTime.of(2026, 7, 1, 10, 0));
        return goal;
    }

    private Category category(String name, TransactionType type) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName(name);
        category.setType(type);
        category.setColor("#123ABC");
        category.setActive(true);
        return category;
    }

    private Transaction transaction(String amount, TransactionType type, Category category) {
        Transaction transaction = new Transaction();
        transaction.setDescription("Lancamento");
        transaction.setAmount(new BigDecimal(amount));
        transaction.setType(type);
        transaction.setCategory(category);
        transaction.setTransactionDate(LocalDate.of(2026, 7, 10));
        return transaction;
    }
}

