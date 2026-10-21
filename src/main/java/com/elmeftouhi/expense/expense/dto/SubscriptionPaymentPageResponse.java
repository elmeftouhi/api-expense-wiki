package com.elmeftouhi.expense.expense.dto;

import java.util.List;

public record SubscriptionPaymentPageResponse(
        List<SubscriptionPaymentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}


