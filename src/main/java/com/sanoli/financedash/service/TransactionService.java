package com.sanoli.financedash.service;

import com.sanoli.financedash.domain.Category;
import com.sanoli.financedash.domain.Transaction;
import com.sanoli.financedash.domain.TransactionType;
import com.sanoli.financedash.dto.TransactionRequest;
import com.sanoli.financedash.dto.TransactionResponse;
import com.sanoli.financedash.exception.BusinessException;
import com.sanoli.financedash.exception.ResourceNotFoundException;
import com.sanoli.financedash.repository.CategoryRepository;
import com.sanoli.financedash.repository.TransactionRepository;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        Transaction transaction = new Transaction();
        applyRequest(transaction, request);
        return TransactionResponse.fromEntity(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> findAll(Integer month, Integer year, TransactionType type, UUID categoryId, Pageable pageable) {
        Specification<Transaction> specification = buildSpecification(month, year, type, categoryId);
        return transactionRepository.findAll(specification, pageable)
                .map(TransactionResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(UUID id) {
        return TransactionResponse.fromEntity(findEntityById(id));
    }

    @Transactional
    public TransactionResponse update(UUID id, TransactionRequest request) {
        Transaction transaction = findEntityById(id);
        applyRequest(transaction, request);
        return TransactionResponse.fromEntity(transactionRepository.save(transaction));
    }

    @Transactional
    public void delete(UUID id) {
        Transaction transaction = findEntityById(id);
        transactionRepository.delete(transaction);
    }

    private Transaction findEntityById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction não encontrada: " + id));
    }

    private void applyRequest(Transaction transaction, TransactionRequest request) {
        Category category = findActiveCategoryById(request.categoryId());
        validateCategoryType(category, request.type());

        transaction.setDescription(request.description());
        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setCategory(category);
        transaction.setTransactionDate(request.transactionDate());
        transaction.setPaymentMethod(request.paymentMethod());
        transaction.setNotes(request.notes());
    }

    private Category findActiveCategoryById(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category não encontrada: " + categoryId));

        if (!category.isActive()) {
            throw new ResourceNotFoundException("Category não encontrada: " + categoryId);
        }

        return category;
    }

    private void validateCategoryType(Category category, TransactionType transactionType) {
        if (category.getType() != transactionType) {
            throw new BusinessException("O tipo da category deve ser igual ao type da transaction");
        }
    }

    private Specification<Transaction> buildSpecification(Integer month, Integer year, TransactionType type, UUID categoryId) {
        validateMonthYearFilter(month, year);

        return Specification.where(byMonthAndYear(month, year))
                .and(byType(type))
                .and(byCategoryId(categoryId));
    }

    private void validateMonthYearFilter(Integer month, Integer year) {
        if ((month == null) != (year == null)) {
            throw new BusinessException("month e year devem ser informados juntos");
        }

        if (month != null && (month < 1 || month > 12)) {
            throw new BusinessException("month deve estar entre 1 e 12");
        }
    }

    private Specification<Transaction> byMonthAndYear(Integer month, Integer year) {
        return (root, query, criteriaBuilder) -> {
            if (month == null || year == null) {
                return criteriaBuilder.conjunction();
            }

            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();
            return criteriaBuilder.between(root.get("transactionDate"), startDate, endDate);
        };
    }

    private Specification<Transaction> byType(TransactionType type) {
        return (root, query, criteriaBuilder) -> type == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("type"), type);
    }

    private Specification<Transaction> byCategoryId(UUID categoryId) {
        return (root, query, criteriaBuilder) -> {
            if (categoryId == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(root.join("category", JoinType.INNER).get("id"), categoryId);
        };
    }
}

