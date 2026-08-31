package com.elmeftouhi.expense.exercice.dto;

import com.elmeftouhi.expense.exercice.ExerciceStatus;
import jakarta.validation.constraints.NotNull;

public record ExerciceStatusUpdateRequest(
        @NotNull ExerciceStatus status
) {
}
