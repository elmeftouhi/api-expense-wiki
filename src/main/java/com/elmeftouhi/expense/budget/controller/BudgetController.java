package com.elmeftouhi.expense.budget.controller;

import com.elmeftouhi.expense.budget.service.BudgetService;
import com.elmeftouhi.expense.budget.dto.BudgetCreateRequest;
import com.elmeftouhi.expense.budget.dto.BudgetPeriodResponse;
import com.elmeftouhi.expense.budget.dto.BudgetPeriodUpdateRequest;
import com.elmeftouhi.expense.budget.dto.BudgetResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public List<BudgetResponse> findAll() {
        return budgetService.findAll();
    }

    @GetMapping("/{id}")
    public BudgetResponse getById(@PathVariable Long id) {
        return budgetService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse create(@Valid @RequestBody BudgetCreateRequest request) {
        return budgetService.create(request);
    }

    @PutMapping("/{id}")
    public BudgetResponse update(@PathVariable Long id, @Valid @RequestBody BudgetCreateRequest request) {
        return budgetService.update(id, request);
    }

    @GetMapping("/{id}/periods")
    public List<BudgetPeriodResponse> getPeriods(@PathVariable Long id) {
        return budgetService.getPeriods(id);
    }

    @GetMapping("/{id}/history")
    public List<BudgetPeriodResponse> getHistory(
            @PathVariable Long id,
            @RequestParam(required = false) Integer monthsBack) {
        return budgetService.getHistory(id, monthsBack);
    }

    @PatchMapping("/{id}/periods/{periodId}")
    public BudgetPeriodResponse updatePeriod(
            @PathVariable Long id,
            @PathVariable Long periodId,
            @Valid @RequestBody BudgetPeriodUpdateRequest request) {
        return budgetService.updatePeriod(id, periodId, request);
    }
}

