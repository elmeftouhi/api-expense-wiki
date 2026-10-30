package com.elmeftouhi.expense.dashboard.service;

import com.elmeftouhi.expense.budget.repository.BudgetPeriodInstanceRepository;
import com.elmeftouhi.expense.dashboard.dto.DashboardSummaryResponse;
import com.elmeftouhi.expense.expense.ExpenseStatus;
import com.elmeftouhi.expense.expense.entity.Expense;
import com.elmeftouhi.expense.expense.repository.ExpenseRepository;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Set<ExpenseStatus> TRACKED_EXPENSE_STATUSES = EnumSet.of(
            ExpenseStatus.PAID,
            ExpenseStatus.PENDING,
            ExpenseStatus.OVERDUE
    );

    private final ExpenseRepository expenseRepository;
    private final BudgetPeriodInstanceRepository budgetPeriodInstanceRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        Long tenantId = TenantContext.getRequiredTenantId();
        LocalDate today = LocalDate.now();

        DashboardSummaryResponse.ComparisonCard monthExpenses = buildMonthComparison(tenantId, today);
        DashboardSummaryResponse.ComparisonCard yearExpenses = buildYearComparison(tenantId, today);
        DashboardSummaryResponse.UpcomingCard upcomingThisMonth = buildUpcomingThisMonth(tenantId, today);
        DashboardSummaryResponse.AmountCountCard overdue = buildOverdue(tenantId, today);
        DashboardSummaryResponse.AmountCountCard pending = buildPending(tenantId, today);
        DashboardSummaryResponse.BudgetCard monthlyBudget = buildBudget(tenantId, today);

        return new DashboardSummaryResponse(
                monthExpenses,
                yearExpenses,
                upcomingThisMonth,
                overdue,
                pending,
                monthlyBudget
        );
    }

    private DashboardSummaryResponse.ComparisonCard buildMonthComparison(Long tenantId, LocalDate today) {
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth previousMonth = currentMonth.minusMonths(1);

        BigDecimal currentTotal = expenseRepository.sumByTenantAndStatusesAndDueDateRange(
                tenantId,
                TRACKED_EXPENSE_STATUSES,
                currentMonth.atDay(1),
                currentMonth.atEndOfMonth());

        BigDecimal previousTotal = expenseRepository.sumByTenantAndStatusesAndDueDateRange(
                tenantId,
                TRACKED_EXPENSE_STATUSES,
                previousMonth.atDay(1),
                previousMonth.atEndOfMonth());

        return new DashboardSummaryResponse.ComparisonCard(
                currentTotal,
                previousTotal,
                computePercentageChange(currentTotal, previousTotal)
        );
    }

    private DashboardSummaryResponse.ComparisonCard buildYearComparison(Long tenantId, LocalDate today) {
        int year = today.getYear();

        BigDecimal currentTotal = expenseRepository.sumByTenantAndStatusesAndDueDateRange(
                tenantId,
                TRACKED_EXPENSE_STATUSES,
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31));

        BigDecimal previousTotal = expenseRepository.sumByTenantAndStatusesAndDueDateRange(
                tenantId,
                TRACKED_EXPENSE_STATUSES,
                LocalDate.of(year - 1, 1, 1),
                LocalDate.of(year - 1, 12, 31));

        return new DashboardSummaryResponse.ComparisonCard(
                currentTotal,
                previousTotal,
                computePercentageChange(currentTotal, previousTotal)
        );
    }

    private DashboardSummaryResponse.UpcomingCard buildUpcomingThisMonth(Long tenantId, LocalDate today) {
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate endOfMonth = currentMonth.atEndOfMonth();

        BigDecimal totalAmount = expenseRepository.sumByTenantAndStatusAndDateRange(
                tenantId,
                ExpenseStatus.PENDING,
                today,
                endOfMonth);

        long count = expenseRepository.countByTenantIdAndStatusAndDueDateBetweenAndDeletedFalse(
                tenantId,
                ExpenseStatus.PENDING,
                today,
                endOfMonth);

        String nextLabel = expenseRepository
                .findFirstByTenantIdAndStatusAndDueDateBetweenAndDeletedFalseOrderByDueDateAsc(
                        tenantId,
                        ExpenseStatus.PENDING,
                        today,
                        endOfMonth)
                .map(this::resolveNextExpenseLabel)
                .orElse(null);

        return new DashboardSummaryResponse.UpcomingCard(totalAmount, count, nextLabel);
    }

    private DashboardSummaryResponse.AmountCountCard buildOverdue(Long tenantId, LocalDate today) {
        Set<ExpenseStatus> overdueStatuses = EnumSet.of(ExpenseStatus.PENDING, ExpenseStatus.OVERDUE);

        BigDecimal totalAmount = expenseRepository.sumByTenantAndStatusesAndDueDateBefore(
                tenantId,
                overdueStatuses,
                today);

        long count = expenseRepository.countByTenantAndStatusesAndDueDateBefore(
                tenantId,
                overdueStatuses,
                today);

        return new DashboardSummaryResponse.AmountCountCard(totalAmount, count);
    }

    private DashboardSummaryResponse.AmountCountCard buildPending(Long tenantId, LocalDate today) {
        BigDecimal totalAmount = expenseRepository.sumByTenantAndStatusAndDueDateOnOrAfter(
                tenantId,
                ExpenseStatus.PENDING,
                today);

        long count = expenseRepository.countByTenantAndStatusAndDueDateOnOrAfter(
                tenantId,
                ExpenseStatus.PENDING,
                today);

        return new DashboardSummaryResponse.AmountCountCard(totalAmount, count);
    }

    private DashboardSummaryResponse.BudgetCard buildBudget(Long tenantId, LocalDate today) {
        BudgetPeriodInstanceRepository.CurrentBudgetTotalsView totals =
                budgetPeriodInstanceRepository.getCurrentTotals(tenantId, today);

        BigDecimal totalBudget = defaultIfNull(totals != null ? totals.getAllocatedAmount() : null);
        BigDecimal spent = defaultIfNull(totals != null ? totals.getSpentAmount() : null);
        BigDecimal remaining = totalBudget.subtract(spent);
        BigDecimal usedPercentage = computeUsedPercentage(spent, totalBudget);

        return new DashboardSummaryResponse.BudgetCard(totalBudget, usedPercentage, remaining);
    }

    private BigDecimal computePercentageChange(BigDecimal current, BigDecimal previous) {
        BigDecimal safeCurrent = defaultIfNull(current);
        BigDecimal safePrevious = defaultIfNull(previous);

        if (safePrevious.compareTo(BigDecimal.ZERO) == 0) {
            return safeCurrent.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(100);
        }

        return safeCurrent
                .subtract(safePrevious)
                .multiply(BigDecimal.valueOf(100))
                .divide(safePrevious, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeUsedPercentage(BigDecimal spent, BigDecimal budget) {
        BigDecimal safeBudget = defaultIfNull(budget);
        if (safeBudget.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return defaultIfNull(spent)
                .multiply(BigDecimal.valueOf(100))
                .divide(safeBudget, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String resolveNextExpenseLabel(Expense expense) {
        if (expense.getDescription() == null || expense.getDescription().isBlank()) {
            return "Expense #" + expense.getId();
        }
        return expense.getDescription();
    }
}

