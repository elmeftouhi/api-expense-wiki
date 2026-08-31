package com.elmeftouhi.expense.user.dto;

import com.elmeftouhi.expense.user.TenantRole;
import jakarta.validation.constraints.NotNull;

public record TenantAssignmentRequest(
        @NotNull Long tenantId,
        @NotNull TenantRole role
) {
}

