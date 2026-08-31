package com.elmeftouhi.expense.category.dto;

import java.math.BigDecimal;
import java.util.List;

public record CategoryResponse(
        Long id,
        Long parentCategoryId,
        Integer level,
        String name,
        String description,
        String icon,
        String textColor,
        String backgroundColor,
        BigDecimal monthlyBudget,
        boolean active,
        boolean allowOverBudget,
        Long tenantId,
        List<CategoryResponse> subCategories
) {
}

