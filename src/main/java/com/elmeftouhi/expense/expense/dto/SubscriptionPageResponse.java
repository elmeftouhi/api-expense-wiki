package com.elmeftouhi.expense.expense.dto;

import java.util.List;

public record SubscriptionPageResponse(
        List<SubscriptionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}

