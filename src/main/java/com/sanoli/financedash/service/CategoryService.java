package com.sanoli.financedash.service;

import com.sanoli.financedash.domain.Category;
import com.sanoli.financedash.dto.CategoryRequest;
import com.sanoli.financedash.dto.CategoryResponse;
import com.sanoli.financedash.exception.BusinessException;
import com.sanoli.financedash.exception.ResourceNotFoundException;
import com.sanoli.financedash.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        validateUniqueName(request.name(), null);

        Category category = new Category();
        applyRequest(category, request);
        return CategoryResponse.fromEntity(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllByActiveTrueOrderByNameAsc()
                .stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(UUID id) {
        return CategoryResponse.fromEntity(findActiveEntityById(id));
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = findActiveEntityById(id);
        validateUniqueName(request.name(), id);
        applyRequest(category, request);
        return CategoryResponse.fromEntity(categoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID id) {
        Category category = findActiveEntityById(id);
        category.setActive(false);
        categoryRepository.save(category);
    }

    private Category findActiveEntityById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category não encontrada: " + id));

        if (!category.isActive()) {
            throw new ResourceNotFoundException("Category não encontrada: " + id);
        }

        return category;
    }

    private void validateUniqueName(String name, UUID currentId) {
        categoryRepository.findByNameIgnoreCase(name.trim())
                .filter(category -> currentId == null || !category.getId().equals(currentId))
                .ifPresent(category -> {
                    throw new BusinessException("Já existe uma category com o nome: " + name);
                });
    }

    private void applyRequest(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setType(request.type());
        category.setColor(request.color());
    }
}

