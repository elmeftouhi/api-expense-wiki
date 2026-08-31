package com.elmeftouhi.expense.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CategoryCreateRequest(
        @Positive Long parentCategoryId,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @Size(max = 100) String icon,
        @Size(max = 20) String textColor,
        @Size(max = 20) String backgroundColor,
        @PositiveOrZero BigDecimal monthlyBudget,
        @PositiveOrZero Integer level,
        @NotNull Boolean active,
        @NotNull Boolean allowOverBudget
) {
}

