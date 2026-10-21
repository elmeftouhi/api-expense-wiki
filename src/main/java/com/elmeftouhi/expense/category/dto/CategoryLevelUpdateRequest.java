package com.elmeftouhi.expense.category.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CategoryLevelUpdateRequest(
        @NotNull @Positive Integer level
) {
}

