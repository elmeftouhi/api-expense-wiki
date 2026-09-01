package com.elmeftouhi.expense.expense.dto;

import com.elmeftouhi.expense.expense.ExpenseType;
import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ExpenseCreateRequest(
        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @NotNull(message = "Due date is required")
        LocalDate dueDate,

        ExpenseType type,

        String paymentMethod,

        String notes
) {
}

