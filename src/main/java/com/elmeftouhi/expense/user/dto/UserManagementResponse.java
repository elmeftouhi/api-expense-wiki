package com.elmeftouhi.expense.user.dto;

import com.elmeftouhi.expense.user.Role;
import com.elmeftouhi.expense.user.UserStatus;
import com.elmeftouhi.expense.tenant.dto.TenantMembershipResponse;
import java.util.List;
import java.util.Set;

public record UserManagementResponse(
        Long id,
        String email,
        String displayedName,
        String firstName,
        String lastName,
        String phone,
        UserStatus status,
        Long defaultTenantId,
        String defaultTenantName,
        Set<Role> globalRoles,
        List<TenantMembershipResponse> tenantMemberships
) {
}


