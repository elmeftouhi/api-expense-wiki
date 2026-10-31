package com.elmeftouhi.expense.datatransfer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ImportUserDataRequest(
        @NotBlank String tenantName,
        @NotNull @Valid UserDataBackupSnapshot backup,
        boolean switchDefaultTenant
) {
}

