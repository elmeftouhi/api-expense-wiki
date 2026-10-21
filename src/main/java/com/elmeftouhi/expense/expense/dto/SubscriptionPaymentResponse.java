package com.elmeftouhi.expense.expense.dto;

import com.elmeftouhi.expense.expense.ExpenseStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SubscriptionPaymentResponse(
        Long id,
        Long subscriptionId,
        LocalDate scheduledBillingDate,
        Long expenseId,
        BigDecimal expenseAmount,
        String expenseCurrency,
        ExpenseStatus expenseStatus,
        LocalDate expenseDueDate,
        LocalDate expensePaidDate,
        Instant createdAt
) {
}


