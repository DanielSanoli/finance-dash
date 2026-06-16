package com.sanoli.financedash.service;

import com.sanoli.financedash.domain.Category;
import com.sanoli.financedash.domain.TransactionType;
import com.sanoli.financedash.domain.AppUser;
import com.sanoli.financedash.dto.CategoryRequest;
import com.sanoli.financedash.dto.CategoryResponse;
import com.sanoli.financedash.exception.BusinessException;
import com.sanoli.financedash.exception.ResourceNotFoundException;
import com.sanoli.financedash.repository.CategoryRepository;
import com.sanoli.financedash.security.CurrentUserService;
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

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void shouldCreateCategory() {
        AppUser user = user();
        CategoryRequest request = new CategoryRequest("Software", TransactionType.EXPENSE, "#8B5CF6");
        UUID id = UUID.randomUUID();
        when(currentUserService.getCurrentUserId()).thenReturn(user.getId());
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByUserIdAndNameIgnoreCase(user.getId(), "Software")).thenReturn(Optional.empty());
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
        assertThat(captor.getValue().getUser()).isEqualTo(user);
    }

    @Test
    void shouldListOnlyActiveCategoriesOrderedByName() {
        AppUser user = user();
        Category alimentação = category(UUID.randomUUID(), "Alimentação", TransactionType.EXPENSE, "#F97316", true);
        Category salario = category(UUID.randomUUID(), "Salário", TransactionType.INCOME, "#16A34A", true);
        when(currentUserService.getCurrentUserId()).thenReturn(user.getId());
        when(categoryRepository.findAllByUserIdAndActiveTrueOrderByNameAsc(user.getId())).thenReturn(List.of(alimentação, salario));

        List<CategoryResponse> responses = categoryService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(CategoryResponse::name)
                .containsExactly("Alimentação", "Salário");
    }

    @Test
    void shouldThrowWhenCategoryIdDoesNotExist() {
        AppUser user = user();
        UUID id = UUID.randomUUID();
        when(currentUserService.getCurrentUserId()).thenReturn(user.getId());
        when(categoryRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category não encontrada");
    }

    @Test
    void shouldThrowWhenCategoryNameAlreadyExists() {
        AppUser user = user();
        Category existing = category(UUID.randomUUID(), "Software", TransactionType.EXPENSE, "#8B5CF6", true);
        CategoryRequest request = new CategoryRequest("software", TransactionType.EXPENSE, "#7C3AED");
        when(currentUserService.getCurrentUserId()).thenReturn(user.getId());
        when(categoryRepository.findByUserIdAndNameIgnoreCase(user.getId(), "software")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe uma category com o nome");
    }

    @Test
    void shouldUpdateCategory() {
        AppUser user = user();
        UUID id = UUID.randomUUID();
        Category category = category(id, "Cartão", TransactionType.EXPENSE, "#EF4444", true);
        CategoryRequest request = new CategoryRequest("Cartão de Crédito", TransactionType.EXPENSE, "#DC2626");
        when(currentUserService.getCurrentUserId()).thenReturn(user.getId());
        when(categoryRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.of(category));
        when(categoryRepository.findByUserIdAndNameIgnoreCase(user.getId(), "Cartão de Crédito")).thenReturn(Optional.empty());
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryResponse response = categoryService.update(id, request);

        assertThat(response.name()).isEqualTo("Cartão de Crédito");
        assertThat(response.color()).isEqualTo("#DC2626");
        verify(categoryRepository).save(category);
    }

    @Test
    void shouldSoftDeleteCategory() {
        AppUser user = user();
        UUID id = UUID.randomUUID();
        Category category = category(id, "Impostos", TransactionType.EXPENSE, "#64748B", true);
        when(currentUserService.getCurrentUserId()).thenReturn(user.getId());
        when(categoryRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.of(category));
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

    private AppUser user() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setName("Daniel");
        user.setEmail("daniel@example.com");
        user.setPasswordHash("hash");
        return user;
    }
}

