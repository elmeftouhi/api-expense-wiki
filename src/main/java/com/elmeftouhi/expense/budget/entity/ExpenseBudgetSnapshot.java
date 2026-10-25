package com.elmeftouhi.expense.budget.entity;

import com.elmeftouhi.expense.expense.entity.Expense;
import com.elmeftouhi.expense.expense.ExpenseStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseBudgetSnapshot(
        Long expenseId,
        Long tenantId,
        Long userId,
        Long categoryId,
        BigDecimal amount,
        ExpenseStatus status,
        LocalDate dueDate
) {
    public static ExpenseBudgetSnapshot fromExpense(Expense expense) {
        return new ExpenseBudgetSnapshot(
                expense.getId(),
                expense.getTenantId(),
                expense.getUserId(),
                expense.getCategory().getId(),
                expense.getAmount(),
                expense.getStatus(),
                expense.getDueDate()
        );
    }
}

