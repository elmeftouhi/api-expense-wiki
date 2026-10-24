package com.elmeftouhi.expense.paymentmethod.controller;

import com.elmeftouhi.expense.paymentmethod.dto.PaymentMethodRequest;
import com.elmeftouhi.expense.paymentmethod.dto.PaymentMethodResponse;
import com.elmeftouhi.expense.paymentmethod.service.PaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payment-methods")
@RequiredArgsConstructor
@Tag(name = "Payment Methods", description = "Manage tenant payment methods and default selection")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    @Operation(summary = "List payment methods", description = "Returns all payment methods, optionally including inactive entries")
    @ApiResponse(responseCode = "200", description = "Payment methods returned successfully")
    public List<PaymentMethodResponse> findAll(
            @Parameter(description = "Include inactive payment methods", example = "false")
            @RequestParam(name = "showInactive", defaultValue = "false") boolean showInactive) {
        return paymentMethodService.findAll(showInactive);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create payment method", description = "Creates a payment method and can set it as default")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment method created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    public PaymentMethodResponse create(@Valid @RequestBody PaymentMethodRequest request) {
        return paymentMethodService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update payment method", description = "Updates payment method details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment method updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Payment method not found")
    })
    public PaymentMethodResponse update(@PathVariable Long id, @Valid @RequestBody PaymentMethodRequest request) {
        return paymentMethodService.update(id, request);
    }

    @PatchMapping("/{id}/set-default")
    @Operation(summary = "Set default payment method", description = "Marks the specified payment method as default")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Default payment method updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Payment method not found")
    })
    public PaymentMethodResponse setDefault(@PathVariable Long id) {
        return paymentMethodService.setDefault(id);
    }
}

