package com.sanoli.financedash.service;

import com.sanoli.financedash.domain.Category;
import com.sanoli.financedash.domain.AppUser;
import com.sanoli.financedash.domain.Transaction;
import com.sanoli.financedash.domain.TransactionType;
import com.sanoli.financedash.dto.CategoryAmountResponse;
import com.sanoli.financedash.dto.MonthlyDashboardResponse;
import com.sanoli.financedash.repository.TransactionRepository;
import com.sanoli.financedash.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void shouldCalculateMonthlyDashboard() {
        AppUser user = user();
        Category salario = category("Salario", TransactionType.INCOME, "#16A34A");
        Category servicos = category("Servicos", TransactionType.INCOME, "#22C55E");
        Category alimentacao = category("Alimentacao", TransactionType.EXPENSE, "#F97316");
        Category cartao = category("Cartao", TransactionType.EXPENSE, "#EF4444");

        when(currentUserService.getCurrentUserId()).thenReturn(user.getId());
        when(transactionRepository.findByUserIdAndTransactionDateBetween(
                user.getId(),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        )).thenReturn(List.of(
                transaction("Salario", "7200.00", TransactionType.INCOME, salario),
                transaction("Freela", "1000.00", TransactionType.INCOME, servicos),
                transaction("Mercado", "850.50", TransactionType.EXPENSE, alimentacao),
                transaction("Cartao", "2193.00", TransactionType.EXPENSE, cartao),
                transaction("Cartao extra", "1233.48", TransactionType.EXPENSE, cartao)
        ));

        MonthlyDashboardResponse response = dashboardService.getMonthlyDashboard(7, 2026);

        assertThat(response.month()).isEqualTo(7);
        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 7, 31));
        assertThat(response.totalIncome()).isEqualByComparingTo("8200.00");
        assertThat(response.totalExpense()).isEqualByComparingTo("4276.98");
        assertThat(response.balance()).isEqualByComparingTo("3923.02");
        assertThat(response.transactionCount()).isEqualTo(5);
        assertThat(response.incomeCount()).isEqualTo(2);
        assertThat(response.expenseCount()).isEqualTo(3);
        assertThat(response.expensesByCategory())
                .containsExactly(
                        new CategoryAmountResponse(alimentacao.getId(), "Alimentacao", "#F97316", new BigDecimal("850.50"), new BigDecimal("19.89")),
                        new CategoryAmountResponse(cartao.getId(), "Cartao", "#EF4444", new BigDecimal("3426.48"), new BigDecimal("80.11"))
                );
        assertThat(response.incomesByCategory())
                .containsExactly(
                        new CategoryAmountResponse(salario.getId(), "Salario", "#16A34A", new BigDecimal("7200.00"), new BigDecimal("87.80")),
                        new CategoryAmountResponse(servicos.getId(), "Servicos", "#22C55E", new BigDecimal("1000.00"), new BigDecimal("12.20"))
                );
    }

    @Test
    void shouldReturnZeroValuesWhenThereAreNoTransactions() {
        AppUser user = user();
        when(currentUserService.getCurrentUserId()).thenReturn(user.getId());
        when(transactionRepository.findByUserIdAndTransactionDateBetween(
                user.getId(),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
        )).thenReturn(List.of());

        MonthlyDashboardResponse response = dashboardService.getMonthlyDashboard(8, 2026);

        assertThat(response.totalIncome()).isEqualByComparingTo("0.00");
        assertThat(response.totalExpense()).isEqualByComparingTo("0.00");
        assertThat(response.balance()).isEqualByComparingTo("0.00");
        assertThat(response.transactionCount()).isZero();
        assertThat(response.expensesByCategory()).isEmpty();
        assertThat(response.incomesByCategory()).isEmpty();
    }

    private Transaction transaction(String description, String amount, TransactionType type, Category category) {
        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setType(type);
        transaction.setCategory(category);
        transaction.setTransactionDate(LocalDate.of(2026, 7, 10));
        return transaction;
    }

    private Category category(String name, TransactionType type, String color) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName(name);
        category.setType(type);
        category.setColor(color);
        category.setActive(true);
        return category;
    }

    private AppUser user() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setName("Daniel");
        user.setEmail("daniel@example.com");
        user.setPasswordHash("hash");
        return user;
    }
}

