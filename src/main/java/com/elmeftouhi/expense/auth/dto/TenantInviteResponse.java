package com.elmeftouhi.expense.auth.dto;

import com.elmeftouhi.expense.user.TenantRole;
import java.time.Instant;

public record TenantInviteResponse(
        String inviteCode,
        Long tenantId,
        Instant expiresAt,
        TenantRole role
) {
}

