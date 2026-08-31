package com.elmeftouhi.expense.tenant.dto;

import com.elmeftouhi.expense.user.TenantRole;

public record TenantMembershipResponse(
        Long id,
        String name,
        boolean isDefault,
        TenantRole role
) {
}

