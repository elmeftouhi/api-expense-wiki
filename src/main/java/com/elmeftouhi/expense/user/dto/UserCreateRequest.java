package com.elmeftouhi.expense.user.dto;

import com.elmeftouhi.expense.user.UserStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UserCreateRequest(
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Size(min = 8, max = 120) String password,
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 40) String phone,
        UserStatus status,
        Long defaultTenantId,
        @NotEmpty @Valid List<TenantAssignmentRequest> tenantAssignments
) {
}

