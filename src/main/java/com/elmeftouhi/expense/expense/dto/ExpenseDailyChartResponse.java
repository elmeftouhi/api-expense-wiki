package com.elmeftouhi.expense.expense.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseDailyChartResponse(
        Integer year,
        Integer month,
        List<DayTotal> totals
) {
    public record DayTotal(
            Integer day,
            BigDecimal total
    ) {
    }
}

