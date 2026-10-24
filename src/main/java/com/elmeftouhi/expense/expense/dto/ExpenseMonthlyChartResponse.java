package com.elmeftouhi.expense.expense.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseMonthlyChartResponse(
        Integer year,
        List<MonthTotal> totals,
        List<MonthTotal> previousYearTotals
) {
    public record MonthTotal(
            Integer month,
            BigDecimal total
    ) {
    }
}

