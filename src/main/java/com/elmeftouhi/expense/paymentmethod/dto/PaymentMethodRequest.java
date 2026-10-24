package com.elmeftouhi.expense.paymentmethod.dto;

import com.elmeftouhi.expense.paymentmethod.PaymentMethodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentMethodRequest(
        @NotBlank(message = "Payment method name is required")
        String name,

        @NotNull(message = "Payment method type is required")
        PaymentMethodType type,

        Boolean active,
        Boolean isDefault
) {
}

