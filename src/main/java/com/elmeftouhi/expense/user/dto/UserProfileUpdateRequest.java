package com.elmeftouhi.expense.user.dto;

import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 40) String phone,
        Long defaultTenantId
) {
}
