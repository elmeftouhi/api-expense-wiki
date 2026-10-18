package com.elmeftouhi.expense.budget.entity;

import com.elmeftouhi.expense.expense.entity.Expense;
import java.math.BigDecimal;
import java.time.Instant;

public record ExpenseBudgetSnapshot(
        Long expenseId,
        Long tenantId,
        Long userId,
        Long categoryId,
        BigDecimal amount,
        Instant createdAt
) {
    public static ExpenseBudgetSnapshot fromExpense(Expense expense) {
        return new ExpenseBudgetSnapshot(
                expense.getId(),
                expense.getTenantId(),
                expense.getUserId(),
                expense.getCategory().getId(),
                expense.getAmount(),
                expense.getCreatedAt()
        );
    }
}

