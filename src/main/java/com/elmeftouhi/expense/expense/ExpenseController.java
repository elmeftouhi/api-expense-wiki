package com.elmeftouhi.expense.expense;

import com.elmeftouhi.expense.expense.dto.ExpenseCreateRequest;
import com.elmeftouhi.expense.expense.dto.ExpenseResponse;
import com.elmeftouhi.expense.expense.dto.ExpenseStatisticsResponse;
import com.elmeftouhi.expense.expense.dto.ExpenseStatusChangeRequest;
import com.elmeftouhi.expense.expense.dto.SubscriptionCreateRequest;
import com.elmeftouhi.expense.expense.dto.SubscriptionResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public Page<ExpenseResponse> findAll(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return expenseService.findAll(year, month, categoryId, pageable);
    }

    @GetMapping("/{id}")
    public ExpenseResponse getById(@PathVariable Long id) {
        return expenseService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@Valid @RequestBody ExpenseCreateRequest request) {
        return expenseService.create(request);
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseCreateRequest request) {
        return expenseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        expenseService.delete(id);
    }

    @PatchMapping("/{id}/status")
    public ExpenseResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseStatusChangeRequest request) {
        return expenseService.changeStatus(id, request.status(), request.paidDate());
    }

    @PatchMapping("/{id}/mark-as-paid")
    public ExpenseResponse markAsPaid(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paidDate) {
        return expenseService.markAsPaid(id, paidDate);
    }

    @GetMapping("/overdue")
    public List<ExpenseResponse> findOverdue() {
        return expenseService.findOverdue();
    }

    @GetMapping("/statistics")
    public ExpenseStatisticsResponse getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        return expenseService.getStatistics(start, end);
    }

    // Subscription endpoints
    @PostMapping("/{expenseId}/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse createSubscription(
            @PathVariable Long expenseId,
            @Valid @RequestBody SubscriptionCreateRequest request) {
        return expenseService.createSubscription(expenseId, request);
    }

    @GetMapping("/{expenseId}/subscriptions")
    public SubscriptionResponse getSubscription(@PathVariable Long expenseId) {
        return expenseService.getSubscription(expenseId);
    }

    @PutMapping("/{expenseId}/subscriptions")
    public SubscriptionResponse updateSubscription(
            @PathVariable Long expenseId,
            @Valid @RequestBody SubscriptionCreateRequest request) {
        return expenseService.updateSubscription(expenseId, request);
    }

    @PatchMapping("/{expenseId}/subscriptions/status")
    public SubscriptionResponse toggleSubscriptionStatus(
            @PathVariable Long expenseId,
            @RequestParam Boolean active) {
        return expenseService.toggleSubscriptionStatus(expenseId, active);
    }
}

