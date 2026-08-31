package com.elmeftouhi.expense.exercice.dto;

import com.elmeftouhi.expense.exercice.ExerciceStatus;
import java.time.LocalDate;

public record ExerciceResponse(
        Long id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        ExerciceStatus status,
        Long tenantId
) {
}
