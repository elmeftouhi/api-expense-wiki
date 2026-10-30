package com.elmeftouhi.expense.dashboard.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        ComparisonCard monthExpenses,
        ComparisonCard yearExpenses,
        UpcomingCard upcomingThisMonth,
        AmountCountCard overdue,
        AmountCountCard pending,
        BudgetCard monthlyBudget
) {
    public record ComparisonCard(
            BigDecimal currentTotal,
            BigDecimal previousTotal,
            BigDecimal percentageChange
    ) {
    }

    public record UpcomingCard(
            BigDecimal totalAmount,
            long count,
            String nextExpenseLabel
    ) {
    }

    public record AmountCountCard(
            BigDecimal totalAmount,
            long count
    ) {
    }

    public record BudgetCard(
            BigDecimal totalBudget,
            BigDecimal usedPercentage,
            BigDecimal totalRemaining
    ) {
    }
}

