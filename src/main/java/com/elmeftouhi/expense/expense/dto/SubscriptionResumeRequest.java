package com.elmeftouhi.expense.expense.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SubscriptionResumeRequest(
        @NotNull(message = "nextBillingDate is required")
        LocalDate nextBillingDate
) {
}

