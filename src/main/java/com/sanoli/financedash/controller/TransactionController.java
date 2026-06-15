package com.sanoli.financedash.controller;

import com.sanoli.financedash.domain.TransactionType;
import com.sanoli.financedash.dto.TransactionRequest;
import com.sanoli.financedash.dto.TransactionResponse;
import com.sanoli.financedash.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @Operation(summary = "Cria um lançamento financeiro")
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(request));
    }

    @GetMapping
    @Operation(summary = "Lista lançamentos financeiros com filtros e paginação")
    public ResponseEntity<Page<TransactionResponse>> findAll(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) UUID categoryId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(transactionService.findAll(month, year, type, categoryId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um lançamento financeiro por ID")
    public ResponseEntity<TransactionResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um lançamento financeiro")
    public ResponseEntity<TransactionResponse> update(@PathVariable UUID id, @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um lançamento financeiro")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

