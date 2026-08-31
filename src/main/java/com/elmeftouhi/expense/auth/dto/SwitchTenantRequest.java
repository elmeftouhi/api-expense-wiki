package com.elmeftouhi.expense.auth.dto;

import jakarta.validation.constraints.NotNull;

public record SwitchTenantRequest(@NotNull Long tenantId) {
}

