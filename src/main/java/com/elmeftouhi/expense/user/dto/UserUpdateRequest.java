package com.elmeftouhi.expense.user.dto;

import com.elmeftouhi.expense.user.UserStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UserUpdateRequest(
        @Email @Size(max = 190) String email,
        @Size(min = 8, max = 120) String password,
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 40) String phone,
        UserStatus status,
        Long defaultTenantId,
        @Valid List<TenantAssignmentRequest> tenantAssignments
) {
}

