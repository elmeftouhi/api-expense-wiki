package com.elmeftouhi.expense.auth.dto;

import com.elmeftouhi.expense.user.TenantRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateTenantInviteRequest(
        @NotNull Long tenantId,
        @NotNull @Min(1) @Max(168) Integer expiresInHours,
        TenantRole role
) {
}

