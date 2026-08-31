package com.elmeftouhi.expense.activity.dto;

import java.util.List;

public record ActivityLogPageResponse(
        List<ActivityLogResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}

