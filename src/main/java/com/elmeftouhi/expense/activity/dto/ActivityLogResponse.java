package com.elmeftouhi.expense.activity.dto;

import com.elmeftouhi.expense.activity.ActivityAction;
import com.elmeftouhi.expense.activity.ActivitySubjectType;
import java.time.Instant;

public record ActivityLogResponse(
        Long id,
        ActivitySubjectType subjectType,
        Long subjectId,
        String subjectLabel,
        ActivityAction action,
        String actorUsername,
        ActivityActorResponse actorUser,
        String message,
        String metadata,
        Instant occurredAt
) {
}

