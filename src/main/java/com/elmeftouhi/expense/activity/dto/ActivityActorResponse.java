package com.elmeftouhi.expense.activity.dto;

public record ActivityActorResponse(
        Long id,
        String email,
        String displayedName,
        String firstName,
        String lastName
) {
}

