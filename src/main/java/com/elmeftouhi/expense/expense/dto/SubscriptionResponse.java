package com.elmeftouhi.expense.expense.dto;

import com.elmeftouhi.expense.expense.SubscriptionFrequency;
import java.time.LocalDate;
import java.time.Instant;

public record SubscriptionResponse(
        Long id,
        Long expenseId,
        SubscriptionFrequency frequency,
        Integer customIntervalDays,
        LocalDate nextOccurrenceDate,
        LocalDate endDate,
        Integer occurrenceCount,
        Boolean active,
        Long tenantId,
        Instant createdAt,
        Instant updatedAt
) {
}

