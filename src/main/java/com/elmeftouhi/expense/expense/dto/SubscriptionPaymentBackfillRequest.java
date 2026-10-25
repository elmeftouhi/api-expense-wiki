package com.elmeftouhi.expense.expense.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SubscriptionPaymentBackfillRequest(
        @NotNull(message = "scheduledBillingDate is required")
        LocalDate scheduledBillingDate,
        LocalDate paidDate
) {
}

