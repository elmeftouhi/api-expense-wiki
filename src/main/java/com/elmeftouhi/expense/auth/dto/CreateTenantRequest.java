package com.elmeftouhi.expense.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank @Size(min = 3, max = 120) String tenantName
) {
}

