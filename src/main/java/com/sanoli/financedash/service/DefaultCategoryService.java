package com.sanoli.financedash.service;

import com.sanoli.financedash.domain.AppUser;
import com.sanoli.financedash.domain.Category;
import com.sanoli.financedash.domain.TransactionType;
import com.sanoli.financedash.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultCategoryService {

    private final CategoryRepository categoryRepository;

    public DefaultCategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public void seedForUser(AppUser user) {
        seedCategory(user, "Salario", TransactionType.INCOME, "#16A34A");
        seedCategory(user, "Servicos", TransactionType.INCOME, "#22C55E");
        seedCategory(user, "Vendas", TransactionType.INCOME, "#14B8A6");
        seedCategory(user, "Alimentacao", TransactionType.EXPENSE, "#F97316");
        seedCategory(user, "Transporte", TransactionType.EXPENSE, "#3B82F6");
        seedCategory(user, "Cartao", TransactionType.EXPENSE, "#EF4444");
        seedCategory(user, "Software", TransactionType.EXPENSE, "#8B5CF6");
        seedCategory(user, "Impostos", TransactionType.EXPENSE, "#64748B");
    }

    private void seedCategory(AppUser user, String name, TransactionType type, String color) {
        if (categoryRepository.existsByUserIdAndNameIgnoreCase(user.getId(), name)) {
            return;
        }

        Category category = new Category();
        category.setUser(user);
        category.setName(name);
        category.setType(type);
        category.setColor(color);
        category.setActive(true);
        categoryRepository.save(category);
    }
}
