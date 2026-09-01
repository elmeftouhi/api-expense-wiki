package com.elmeftouhi.expense.expense.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseStatisticsResponse(
        BigDecimal totalExpenses,
        BigDecimal totalPaid,
        BigDecimal totalPending,
        BigDecimal totalOverdue,
        Integer overdueCount,
        Integer pendingCount,
        List<CategorySummary> byCategory
) {
    public record CategorySummary(
            Long categoryId,
            String categoryName,
            BigDecimal total,
            BigDecimal budget,
            Integer percentage
    ) {
    }
}

