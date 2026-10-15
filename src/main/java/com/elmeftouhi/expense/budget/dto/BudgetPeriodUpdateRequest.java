package com.elmeftouhi.expense.budget.dto;

import com.elmeftouhi.expense.budget.BudgetWarningType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record BudgetPeriodUpdateRequest(
        @NotNull @Positive BigDecimal allocatedAmount,
        @NotNull BudgetWarningType warningType,
        @NotNull @DecimalMin("0.01") BigDecimal warningThreshold
) {
}

