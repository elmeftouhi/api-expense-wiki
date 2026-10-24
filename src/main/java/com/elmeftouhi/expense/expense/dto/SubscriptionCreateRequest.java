package com.elmeftouhi.expense.expense.dto;

import com.elmeftouhi.expense.expense.SubscriptionFrequency;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record SubscriptionCreateRequest(
        @NotBlank(message = "Name is required")
        String name,

        String description,

        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        String currency,

        @NotNull(message = "Frequency is required")
        SubscriptionFrequency frequency,

        @Positive(message = "customIntervalDays must be positive when provided")
        Integer customIntervalDays,

        @NotNull(message = "Next billing date is required")
        LocalDate nextBillingDate,

        LocalDate endDate,

        @PositiveOrZero(message = "Reminder lead time must be zero or positive")
        Integer reminderLeadDays,

        Long paymentMethodId,

        String notes
) {
}

