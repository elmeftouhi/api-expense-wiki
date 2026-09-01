package com.elmeftouhi.expense.expense.dto;

import com.elmeftouhi.expense.expense.SubscriptionFrequency;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;

public record SubscriptionCreateRequest(
        @NotNull(message = "Frequency is required")
        SubscriptionFrequency frequency,

        Integer customIntervalDays,

        LocalDate endDate
) {
}

