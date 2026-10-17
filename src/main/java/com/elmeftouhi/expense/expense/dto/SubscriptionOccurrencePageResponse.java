package com.elmeftouhi.expense.expense.dto;

import java.util.List;

public record SubscriptionOccurrencePageResponse(
        List<SubscriptionOccurrenceResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}

