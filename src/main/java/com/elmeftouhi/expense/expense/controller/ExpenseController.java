package com.elmeftouhi.expense.expense.controller;

import com.elmeftouhi.expense.expense.service.ExpenseService;
import com.elmeftouhi.expense.expense.ExpenseStatus;
import com.elmeftouhi.expense.expense.ExpenseType;
import com.elmeftouhi.expense.expense.dto.ExpenseCreateRequest;
import com.elmeftouhi.expense.expense.dto.ExpenseDailyChartResponse;
import com.elmeftouhi.expense.expense.dto.ExpenseMonthlyChartResponse;
import com.elmeftouhi.expense.expense.dto.ExpensePageResponse;
import com.elmeftouhi.expense.expense.dto.ExpenseResponse;
import com.elmeftouhi.expense.expense.dto.ExpenseStatisticsResponse;
import com.elmeftouhi.expense.expense.dto.ExpenseStatusChangeRequest;
import com.elmeftouhi.expense.shared.exception.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Manage expenses, status changes, and statistics")
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    @Operation(summary = "List expenses", description = "Returns paginated expenses with optional filtering by year, month, category, type, and search text")
    @ApiResponse(responseCode = "200", description = "Expenses returned successfully")
    public ExpensePageResponse findAll(
            @Parameter(description = "Filter by year", example = "2026")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Filter by month (1-12)", example = "10")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Filter by category ID (legacy single-value filter)", example = "5")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter by category IDs", example = "5,8")
            @RequestParam(required = false) List<Long> categoryIds,
            @Parameter(description = "Filter by expense type")
            @RequestParam(required = false) ExpenseType type,
            @Parameter(description = "Filter by payment status (paid or pending)")
            @RequestParam(required = false) ExpenseStatus status,
            @Parameter(description = "Search text applied to description and amount", example = "45")
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.DESC) Pageable pageable) {
        List<Long> effectiveCategoryIds = new ArrayList<>();
        if (categoryIds != null) {
            effectiveCategoryIds.addAll(categoryIds);
        }
        if (categoryId != null) {
            effectiveCategoryIds.add(categoryId);
        }

        ExpenseService.ExpensePageSearchResult pageSearchResult = expenseService.findAllWithAggregates(
                year,
                month,
                effectiveCategoryIds.isEmpty() ? null : effectiveCategoryIds.stream().distinct().toList(),
                type,
                status,
                search,
                pageable);
        Page<ExpenseResponse> pageResult = pageSearchResult.page();
        return new ExpensePageResponse(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageSearchResult.filteredTotalAmount(),
                pageResult.getNumberOfElements()
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get expense by ID", description = "Returns details of a single expense")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Expense returned successfully"),
            @ApiResponse(responseCode = "404", description = "Expense not found")
    })
    public ExpenseResponse getById(@PathVariable Long id) {
        return expenseService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create expense", description = "Creates a new expense record")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Expense created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    public ExpenseResponse create(@Valid @RequestBody ExpenseCreateRequest request) {
        return expenseService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update expense", description = "Updates an existing expense")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Expense updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Expense not found")
    })
    public ExpenseResponse update(
            @Parameter(description = "ID of the expense to update", example = "42")
            @PathVariable Long id,
            @Valid @RequestBody ExpenseCreateRequest request) {
        return expenseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete expense", description = "Deletes an expense")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Expense deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Expense not found")
    })
    public void delete(@PathVariable Long id) {
        expenseService.delete(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change expense status", description = "Changes the status of an expense and optionally sets paid date")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Expense status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Expense not found")
    })
    public ExpenseResponse changeStatus(
            @Parameter(description = "ID of the expense", example = "42")
            @PathVariable Long id,
            @Valid @RequestBody ExpenseStatusChangeRequest request) {
        return expenseService.changeStatus(id, request.status(), request.paidDate());
    }

    @PatchMapping("/{id}/mark-as-paid")
    @Operation(summary = "Mark expense as paid", description = "Marks an expense as paid on the provided date")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Expense marked as paid successfully"),
            @ApiResponse(responseCode = "404", description = "Expense not found")
    })
    public ExpenseResponse markAsPaid(
            @Parameter(description = "ID of the expense", example = "42")
            @PathVariable Long id,
            @Parameter(description = "Payment date (ISO format)", example = "2026-10-11")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paidDate) {
        return expenseService.markAsPaid(id, paidDate);
    }

    @GetMapping("/overdue")
    @Operation(summary = "List overdue expenses", description = "Returns all expenses that are currently overdue")
    @ApiResponse(responseCode = "200", description = "Overdue expenses returned successfully")
    public List<ExpenseResponse> findOverdue() {
        return expenseService.findOverdue();
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get expense statistics", description = "Returns aggregated statistics for a date range")
    @ApiResponse(responseCode = "200", description = "Statistics returned successfully")
    public ExpenseStatisticsResponse getStatistics(
            @Parameter(description = "Start date (ISO format)", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (ISO format)", example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        return expenseService.getStatistics(start, end);
    }

    @GetMapping("/charts/daily")
    @Operation(summary = "Get daily chart totals", description = "Returns day-by-day totals for a given year and month plus previous-month comparison totals, filling missing days with zero")
    @ApiResponse(responseCode = "200", description = "Daily chart totals returned successfully")
    public ExpenseDailyChartResponse getDailyChartTotals(
            @Parameter(description = "Year", example = "2026")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Month (1-12)", example = "10")
            @RequestParam(required = false) Integer month) {
        LocalDate today = LocalDate.now();
        Integer effectiveYear = year != null ? year : today.getYear();
        Integer effectiveMonth = month != null ? month : today.getMonthValue();
        return expenseService.getDailyTotalsForMonth(effectiveYear, effectiveMonth);
    }

    @GetMapping("/charts/monthly")
    @Operation(summary = "Get monthly chart totals", description = "Returns month-by-month totals for a given year and previous year, filling missing months with zero")
    @ApiResponse(responseCode = "200", description = "Monthly chart totals returned successfully")
    public ExpenseMonthlyChartResponse getMonthlyChartTotals(
            @Parameter(description = "Year", example = "2026")
            @RequestParam(required = false) Integer year) {
        Integer effectiveYear = year != null ? year : LocalDate.now().getYear();
        return expenseService.getMonthlyTotalsForYear(effectiveYear);
    }

}

