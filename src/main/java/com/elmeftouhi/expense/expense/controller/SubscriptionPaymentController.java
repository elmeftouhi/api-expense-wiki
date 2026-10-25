package com.elmeftouhi.expense.expense.controller;

import com.elmeftouhi.expense.expense.dto.SubscriptionPaymentPageResponse;
import com.elmeftouhi.expense.expense.dto.SubscriptionPaymentGenerationResponse;
import com.elmeftouhi.expense.expense.dto.SubscriptionPaymentBackfillRequest;
import com.elmeftouhi.expense.expense.dto.SubscriptionPaymentResponse;
import jakarta.validation.Valid;
import com.elmeftouhi.expense.expense.service.SubscriptionPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Subscription Payments", description = "Read generated subscription payment records and linked expenses")
public class SubscriptionPaymentController {

    private final SubscriptionPaymentService subscriptionPaymentService;

    @GetMapping({"/subscriptions/{subscriptionId}/payments", "/subscriptions/{subscriptionId}/occurrences"})
    @Operation(
            summary = "List subscription payments",
            description = "Returns paginated generated payment history for a subscription"
    )
    @ApiResponse(responseCode = "200", description = "Subscription payment history returned successfully")
    public SubscriptionPaymentPageResponse getSubscriptionPayments(
            @PathVariable Long subscriptionId,
            @PageableDefault(size = 20, sort = "scheduledBillingDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<SubscriptionPaymentResponse> pageResult =
                subscriptionPaymentService.getPaymentHistoryBySubscription(subscriptionId, pageable);

        return new SubscriptionPaymentPageResponse(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    @GetMapping("/subscription-payments/{paymentId}")
    @Operation(
            summary = "Get subscription payment by ID",
            description = "Returns one generated subscription payment including its linked expense details"
    )
    @ApiResponse(responseCode = "200", description = "Subscription payment returned successfully")
    public SubscriptionPaymentResponse getSubscriptionPaymentById(@PathVariable Long paymentId) {
        return subscriptionPaymentService.getPaymentById(paymentId);
    }

    @PostMapping("/subscriptions/{subscriptionId}/payments/generate-next")
    @Operation(
            summary = "Generate next subscription payment",
            description = "Generates the next payment for a subscription when it has not been generated yet"
    )
    @ApiResponse(responseCode = "200", description = "Next subscription payment generation attempted successfully")
    public SubscriptionPaymentGenerationResponse generateNextSubscriptionPayment(@PathVariable Long subscriptionId) {
        return subscriptionPaymentService.generateNextPayment(subscriptionId);
    }

    @PostMapping("/subscriptions/{subscriptionId}/payments/backfill")
    @Operation(
            summary = "Backfill subscription payment",
            description = "Creates a manual historical subscription payment and updates next billing when applicable"
    )
    @ApiResponse(responseCode = "200", description = "Backfill payment processed successfully")
    public SubscriptionPaymentGenerationResponse backfillSubscriptionPayment(
            @PathVariable Long subscriptionId,
            @Valid @RequestBody SubscriptionPaymentBackfillRequest request) {
        return subscriptionPaymentService.backfillPayment(subscriptionId, request);
    }
}

