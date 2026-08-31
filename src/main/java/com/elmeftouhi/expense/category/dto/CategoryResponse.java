package com.elmeftouhi.expense.category.dto;

import java.math.BigDecimal;

public record CategoryResponse(
        Long id,
        String name,
        String description,
        String icon,
        String textColor,
        String backgroundColor,
        BigDecimal monthlyBudget,
        boolean active,
        boolean allowOverBudget,
        Long tenantId
) {
}

