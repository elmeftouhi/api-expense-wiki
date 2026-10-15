package com.elmeftouhi.expense.budget.dto;

import com.elmeftouhi.expense.budget.BudgetPeriodStatus;
import com.elmeftouhi.expense.budget.BudgetWarningType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BudgetPeriodResponse(
        Long id,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal allocatedAmount,
        BigDecimal spentAmount,
        BudgetPeriodStatus status,
        BudgetWarningType warningType,
        BigDecimal warningThreshold,
        boolean warningSent,
        boolean exceededSent,
        Instant createdAt,
        Instant updatedAt
) {
}

