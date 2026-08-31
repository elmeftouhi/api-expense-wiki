package com.elmeftouhi.expense.security;

import java.util.Set;

public record JwtPrincipal(
        Long userId,
        String username,
        Long selectedTenantId,
        Set<Long> allowedTenantIds
) {
}

