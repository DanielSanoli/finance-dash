package com.sanoli.financedash.config;

import com.sanoli.financedash.domain.Category;
import com.sanoli.financedash.domain.TransactionType;
import com.sanoli.financedash.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedDefaultCategories(CategoryRepository categoryRepository) {
        return args -> {
            seedCategory(categoryRepository, "Salario", TransactionType.INCOME, "#16A34A");
            seedCategory(categoryRepository, "Servicos", TransactionType.INCOME, "#22C55E");
            seedCategory(categoryRepository, "Vendas", TransactionType.INCOME, "#14B8A6");
            seedCategory(categoryRepository, "Alimentacao", TransactionType.EXPENSE, "#F97316");
            seedCategory(categoryRepository, "Transporte", TransactionType.EXPENSE, "#3B82F6");
            seedCategory(categoryRepository, "Cartao", TransactionType.EXPENSE, "#EF4444");
            seedCategory(categoryRepository, "Software", TransactionType.EXPENSE, "#8B5CF6");
            seedCategory(categoryRepository, "Impostos", TransactionType.EXPENSE, "#64748B");
        };
    }

    private void seedCategory(CategoryRepository categoryRepository, String name, TransactionType type, String color) {
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            return;
        }

        Category category = new Category();
        category.setName(name);
        category.setType(type);
        category.setColor(color);
        category.setActive(true);
        categoryRepository.save(category);
    }
}

