package com.sanoli.financedash.service;

import com.sanoli.financedash.domain.Category;
import com.sanoli.financedash.domain.TransactionType;
import com.sanoli.financedash.dto.CategoryRequest;
import com.sanoli.financedash.dto.CategoryResponse;
import com.sanoli.financedash.exception.BusinessException;
import com.sanoli.financedash.exception.ResourceNotFoundException;
import com.sanoli.financedash.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void shouldCreateCategory() {
        CategoryRequest request = new CategoryRequest("Software", TransactionType.EXPENSE, "#8B5CF6");
        UUID id = UUID.randomUUID();
        when(categoryRepository.findByNameIgnoreCase("Software")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(id);
            category.setCreatedAt(LocalDateTime.of(2026, 7, 10, 10, 0));
            category.setUpdatedAt(LocalDateTime.of(2026, 7, 10, 10, 0));
            return category;
        });

        CategoryResponse response = categoryService.create(request);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("Software");
        assertThat(response.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(response.color()).isEqualTo("#8B5CF6");
        assertThat(response.active()).isTrue();

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Software");
        assertThat(captor.getValue().getColor()).isEqualTo("#8B5CF6");
    }

    @Test
    void shouldListOnlyActiveCategoriesOrderedByName() {
        Category alimentação = category(UUID.randomUUID(), "Alimentação", TransactionType.EXPENSE, "#F97316", true);
        Category salario = category(UUID.randomUUID(), "Salário", TransactionType.INCOME, "#16A34A", true);
        when(categoryRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(alimentação, salario));

        List<CategoryResponse> responses = categoryService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(CategoryResponse::name)
                .containsExactly("Alimentação", "Salário");
    }

    @Test
    void shouldThrowWhenCategoryIdDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category não encontrada");
    }

    @Test
    void shouldThrowWhenCategoryNameAlreadyExists() {
        Category existing = category(UUID.randomUUID(), "Software", TransactionType.EXPENSE, "#8B5CF6", true);
        CategoryRequest request = new CategoryRequest("software", TransactionType.EXPENSE, "#7C3AED");
        when(categoryRepository.findByNameIgnoreCase("software")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe uma category com o nome");
    }

    @Test
    void shouldUpdateCategory() {
        UUID id = UUID.randomUUID();
        Category category = category(id, "Cartão", TransactionType.EXPENSE, "#EF4444", true);
        CategoryRequest request = new CategoryRequest("Cartão de Crédito", TransactionType.EXPENSE, "#DC2626");
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryRepository.findByNameIgnoreCase("Cartão de Crédito")).thenReturn(Optional.empty());
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryResponse response = categoryService.update(id, request);

        assertThat(response.name()).isEqualTo("Cartão de Crédito");
        assertThat(response.color()).isEqualTo("#DC2626");
        verify(categoryRepository).save(category);
    }

    @Test
    void shouldSoftDeleteCategory() {
        UUID id = UUID.randomUUID();
        Category category = category(id, "Impostos", TransactionType.EXPENSE, "#64748B", true);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);

        categoryService.delete(id);

        assertThat(category.isActive()).isFalse();
        verify(categoryRepository).save(category);
    }

    private Category category(UUID id, String name, TransactionType type, String color, boolean active) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setType(type);
        category.setColor(color);
        category.setActive(active);
        category.setCreatedAt(LocalDateTime.of(2026, 7, 10, 10, 0));
        category.setUpdatedAt(LocalDateTime.of(2026, 7, 10, 10, 0));
        return category;
    }
}

