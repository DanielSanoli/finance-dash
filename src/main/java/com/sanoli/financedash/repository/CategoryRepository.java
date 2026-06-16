package com.sanoli.financedash.repository;

import com.sanoli.financedash.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

	List<Category> findAllByUserIdAndActiveTrueOrderByNameAsc(UUID userId);

	Optional<Category> findByUserIdAndNameIgnoreCase(UUID userId, String name);

	Optional<Category> findByIdAndUserId(UUID id, UUID userId);

	boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);
}

