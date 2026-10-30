package com.elmeftouhi.expense.expense.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExpensePageResponse(
        List<ExpenseResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        BigDecimal filteredTotalAmount,
        int recordsCount
) {
}

