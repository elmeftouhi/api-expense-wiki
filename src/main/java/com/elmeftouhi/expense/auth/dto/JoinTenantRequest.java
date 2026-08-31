package com.elmeftouhi.expense.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinTenantRequest(
		@NotBlank @Size(min = 8, max = 64) String inviteCode
) {
}

