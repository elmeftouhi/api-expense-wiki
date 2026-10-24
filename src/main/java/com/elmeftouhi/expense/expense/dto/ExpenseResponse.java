package com.elmeftouhi.expense.expense.dto;

import com.elmeftouhi.expense.expense.ExpenseType;
import com.elmeftouhi.expense.expense.ExpenseStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

public record ExpenseResponse(
        Long id,
        Long categoryId,
        String categoryName,
        Long userId,
        String description,
        BigDecimal amount,
        String currency,
        ExpenseType type,
        ExpenseStatus status,
        LocalDate dueDate,
        LocalDate paidDate,
        Long paymentMethodId,
        String paymentMethodName,
        String notes,
        Boolean hasSubscription,
        Long tenantId,
        Instant createdAt,
        Instant updatedAt
) {
}
