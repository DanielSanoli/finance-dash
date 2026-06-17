package com.sanoli.financedash.service;

import com.sanoli.financedash.domain.Category;
import com.sanoli.financedash.domain.AppUser;
import com.sanoli.financedash.domain.Transaction;
import com.sanoli.financedash.domain.TransactionType;
import com.sanoli.financedash.dto.TransactionRequest;
import com.sanoli.financedash.dto.TransactionResponse;
import com.sanoli.financedash.exception.BusinessException;
import com.sanoli.financedash.exception.ResourceNotFoundException;
import com.sanoli.financedash.repository.CategoryRepository;
import com.sanoli.financedash.repository.TransactionRepository;
import com.sanoli.financedash.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private com.sanoli.financedash.radar.rules.RadarRuleEngine radarRuleEngine;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void shouldCreateTransaction() {
        AppUser user = user();
        Category category = category(UUID.randomUUID(), "Servicos", TransactionType.INCOME);
        TransactionRequest request = new TransactionRequest(
                "Projeto website",
                new BigDecimal("2500.00"),
                TransactionType.INCOME,
                category.getId(),
                LocalDate.of(2026, 7, 10),
                "PIX",
                "Cliente Sanoli"
        );

        UUID id = UUID.randomUUID();
        when(currentUserService.getCurrentUserId()).thenReturn(user.getId());
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByIdAndUserId(category.getId(), user.getId())).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(id);
            transaction.setCreatedAt(LocalDateTime.of(2026, 7, 10, 10, 0));
            transaction.setUpdatedAt(LocalDateTime.of(2026, 7, 10, 10, 0));
            return transaction;
        });

        TransactionResponse response = transactionService.create(request);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.description()).isEqualTo("Projeto website");
        assertThat(response.amount()).isEqualByComparingTo("2500.00");
        assertThat(response.type()).isEqualTo(TransactionType.INCOME);
        assertThat(response.categoryId()).isEqualTo(category.getId());
        assertThat(response.categoryName()).isEqualTo("Servicos");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentMethod()).isEqualTo("PIX");
        assertThat(captor.getValue().getCategory()).isEqualTo(category);
        assertThat(captor.getValue().getUser()).isEqualTo(user);
    }

    @Test
    void shouldListTransactions() {
        AppUser user = user();
        Transaction income = transaction(
                UUID.randomUUID(),
                "Recebimento",
                "7200.00",
                TransactionType.INCOME,
                "Salario"
        );
        Transaction expense = transaction(
                UUID.randomUUID(),
                "Cartao",
                "2193.00",
                TransactionType.EXPENSE,
                "Cartao"
        );
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(income, expense)));
        when(currentUserService.getCurrentUserId()).thenReturn(user.getId());

        Page<TransactionResponse> responses = transactionService.findAll(null, null, null, null, PageRequest.of(0, 20));

        assertThat(responses.getContent()).hasSize(2);
        assertThat(responses.getContent()).extracting(TransactionResponse::description)
                .containsExactly("Recebimento", "Cartao");
    }

    @Test
    void shouldRejectTransactionWhenCategoryTypeDoesNotMatch() {
        AppUser user = user();
        Category category = category(UUID.randomUUID(), "Software", TransactionType.EXPENSE);
        TransactionRequest request = new TransactionRequest(
                "Projeto website",
                new BigDecimal("2500.00"),
                TransactionType.INCOME,
                category.getId(),
                LocalDate.of(2026, 7, 10),
                "PIX",
                "Cliente Sanoli"
        );
        when(currentUserService.getCurrentUserId()).thenReturn(user.getId());
        when(categoryRepository.findByIdAndUserId(category.getId(), user.getId())).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tipo da category");
    }

    @Test
    void shouldThrowWhenTransactionIdDoesNotExist() {
        AppUser user = user();
        UUID id = UUID.randomUUID();
        when(currentUserService.getCurrentUserId()).thenReturn(user.getId());
        when(transactionRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction não encontrada");
    }

    private Transaction transaction(UUID id, String description, String amount, TransactionType type, String category) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setDescription(description);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setType(type);
        transaction.setCategory(category(UUID.randomUUID(), category, type));
        transaction.setTransactionDate(LocalDate.of(2026, 7, 10));
        transaction.setCreatedAt(LocalDateTime.of(2026, 7, 10, 10, 0));
        transaction.setUpdatedAt(LocalDateTime.of(2026, 7, 10, 10, 0));
        return transaction;
    }

    private Category category(UUID id, String name, TransactionType type) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setType(type);
        category.setColor("#123ABC");
        category.setActive(true);
        category.setCreatedAt(LocalDateTime.of(2026, 7, 10, 10, 0));
        category.setUpdatedAt(LocalDateTime.of(2026, 7, 10, 10, 0));
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

