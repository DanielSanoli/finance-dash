package com.sanoli.financedash.controller;

import com.sanoli.financedash.dto.MonthlyDashboardResponse;
import com.sanoli.financedash.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Validated
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/monthly")
    @Operation(summary = "Retorna o dashboard financeiro mensal")
    public ResponseEntity<MonthlyDashboardResponse> getMonthlyDashboard(
            @RequestParam @Min(1) @Max(12) Integer month,
            @RequestParam @Min(1900) Integer year
    ) {
        return ResponseEntity.ok(dashboardService.getMonthlyDashboard(month, year));
    }
}

