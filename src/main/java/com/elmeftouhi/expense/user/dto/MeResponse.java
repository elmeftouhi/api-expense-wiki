package com.elmeftouhi.expense.user.dto;

import com.elmeftouhi.expense.user.Role;
import com.elmeftouhi.expense.user.UserStatus;
import java.util.Set;

public record MeResponse(
        Long id,
        String email,
        String displayedName,
        String firstName,
        String lastName,
        String phone,
        UserStatus status,
        Long defaultTenantId,
        Long currentTenantId,
        Set<Role> roles
) {
}
