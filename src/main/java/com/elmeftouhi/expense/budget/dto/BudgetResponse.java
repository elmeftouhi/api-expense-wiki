package com.elmeftouhi.expense.budget.dto;

import com.elmeftouhi.expense.budget.BudgetPeriodicity;
import com.elmeftouhi.expense.budget.BudgetWarningType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record BudgetResponse(
        Long id,
        String name,
        String description,
        BudgetPeriodicity periodicity,
        BigDecimal allocatedAmount,
        BudgetWarningType warningType,
        BigDecimal warningThreshold,
        boolean active,
        Set<Long> categoryIds,
        Long tenantId,
        BudgetPeriodResponse currentPeriod,
        Instant createdAt,
        Instant updatedAt
) {
}

