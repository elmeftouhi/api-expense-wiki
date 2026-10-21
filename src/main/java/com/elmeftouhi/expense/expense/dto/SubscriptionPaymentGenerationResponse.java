package com.elmeftouhi.expense.expense.dto;

import java.time.LocalDate;

public record SubscriptionPaymentGenerationResponse(
        boolean generated,
        boolean alreadyExists,
        String message,
        Long paymentId,
        Long expenseId,
        LocalDate scheduledBillingDate
) {
}

