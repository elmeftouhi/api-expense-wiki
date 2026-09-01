package com.elmeftouhi.expense.notification.dto;

import com.elmeftouhi.expense.notification.NotificationStatus;
import com.elmeftouhi.expense.notification.NotificationType;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long userId,
        NotificationType type,
        NotificationStatus status,
        String title,
        String message,
        String subjectType,
        Long subjectId,
        String data,
        Long tenantId,
        Instant createdAt,
        Instant readAt,
        Instant archivedAt
) {
}

