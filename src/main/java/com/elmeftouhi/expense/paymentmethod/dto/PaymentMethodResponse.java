package com.elmeftouhi.expense.paymentmethod.dto;

import com.elmeftouhi.expense.paymentmethod.PaymentMethodType;
import java.time.Instant;

public record PaymentMethodResponse(
        Long id,
        String name,
        PaymentMethodType type,
        boolean active,
        boolean isDefault,
        Long tenantId,
        Instant createdAt,
        Instant updatedAt
) {
}

