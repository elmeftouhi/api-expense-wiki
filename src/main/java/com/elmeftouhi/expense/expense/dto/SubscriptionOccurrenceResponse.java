package com.elmeftouhi.expense.expense.dto;

import java.time.Instant;
import java.time.LocalDate;

public record SubscriptionOccurrenceResponse(
        Long id,
        Long subscriptionId,
        Long generatedExpenseId,
        LocalDate occurrenceDate,
        Instant createdAt
) {
}

