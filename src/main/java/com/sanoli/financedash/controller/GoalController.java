package com.sanoli.financedash.controller;

import com.sanoli.financedash.dto.GoalRequest;
import com.sanoli.financedash.dto.GoalResponse;
import com.sanoli.financedash.service.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/goals")
@Validated
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    @Operation(summary = "Cria uma meta financeira")
    public ResponseEntity<GoalResponse> create(@Valid @RequestBody GoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(goalService.create(request));
    }

    @GetMapping
    @Operation(summary = "Lista todas as metas financeiras")
    public ResponseEntity<List<GoalResponse>> findAll() {
        return ResponseEntity.ok(goalService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma meta financeira por ID")
    public ResponseEntity<GoalResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(goalService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma meta financeira")
    public ResponseEntity<GoalResponse> update(@PathVariable UUID id, @Valid @RequestBody GoalRequest request) {
        return ResponseEntity.ok(goalService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma meta financeira")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        goalService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/monthly")
    @Operation(summary = "Lista metas financeiras por mês e ano")
    public ResponseEntity<List<GoalResponse>> findByMonthAndYear(
            @RequestParam @Min(1) @Max(12) Integer month,
            @RequestParam @Min(1900) Integer year
    ) {
        return ResponseEntity.ok(goalService.findByMonthAndYear(month, year));
    }
}

