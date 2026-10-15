package com.elmeftouhi.expense.budget.dto;

import com.elmeftouhi.expense.budget.BudgetPeriodicity;
import com.elmeftouhi.expense.budget.BudgetWarningType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Set;

public record BudgetCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @NotNull BudgetPeriodicity periodicity,
        @NotNull @Positive BigDecimal allocatedAmount,
        @NotNull BudgetWarningType warningType,
        @NotNull @DecimalMin("0.01") BigDecimal warningThreshold,
        @NotEmpty Set<@NotNull @Positive Long> categoryIds,
        @NotNull Boolean active
) {
}

