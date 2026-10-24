package com.elmeftouhi.expense.expense.dto;

import com.elmeftouhi.expense.expense.SubscriptionLifecycleStatus;
import com.elmeftouhi.expense.expense.SubscriptionFrequency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

public record SubscriptionResponse(
        Long id,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        Long ownerUserId,
        BigDecimal amount,
        String currency,
        SubscriptionFrequency frequency,
        Integer customIntervalDays,
        LocalDate nextBillingDate,
        Boolean overdue,
        Long daysLeftBeforeDueDate,
        LocalDate endDate,
        SubscriptionLifecycleStatus status,
        Integer reminderLeadDays,
        Long paymentMethodId,
        String paymentMethodName,
        String notes,
        Long tenantId,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}

