package com.elmeftouhi.expense.dashboard.controller;

import com.elmeftouhi.expense.dashboard.dto.DashboardSummaryResponse;
import com.elmeftouhi.expense.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Aggregated dashboard metrics")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary", description = "Returns aggregate cards for expenses and budgets")
    @ApiResponse(responseCode = "200", description = "Dashboard summary returned successfully")
    public DashboardSummaryResponse getSummary() {
        return dashboardService.getSummary();
    }
}

