package com.elmeftouhi.expense.expense.service;

import com.elmeftouhi.expense.expense.dto.SubscriptionPaymentGenerationResponse;
import com.elmeftouhi.expense.expense.dto.SubscriptionPaymentBackfillRequest;
import com.elmeftouhi.expense.expense.dto.SubscriptionPaymentResponse;
import com.elmeftouhi.expense.expense.entity.SubscriptionPayment;
import com.elmeftouhi.expense.expense.entity.Subscription;
import com.elmeftouhi.expense.expense.repository.SubscriptionPaymentRepository;
import com.elmeftouhi.expense.expense.repository.SubscriptionRepository;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.security.TenantPermissionService;
import com.elmeftouhi.expense.shared.exception.BadRequestException;
import com.elmeftouhi.expense.shared.exception.ResourceNotFoundException;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionPaymentService {

    private final SubscriptionPaymentRepository subscriptionPaymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionGenerationService subscriptionGenerationService;
    private final TenantPermissionService tenantPermissionService;

    @Transactional(readOnly = true)
    public Page<SubscriptionPaymentResponse> getPaymentHistoryBySubscription(Long subscriptionId, Pageable pageable) {
        Long tenantId = TenantContext.getRequiredTenantId();
        subscriptionRepository.findByIdAndTenantId(subscriptionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        return subscriptionPaymentRepository
                .findBySubscriptionIdAndTenantIdOrderByScheduledBillingDateDesc(subscriptionId, tenantId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SubscriptionPaymentResponse getPaymentById(Long paymentId) {
        Long tenantId = TenantContext.getRequiredTenantId();
        SubscriptionPayment payment = subscriptionPaymentRepository.findByIdAndTenantId(paymentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription payment not found"));
        return toResponse(payment);
    }

    @Transactional
    public SubscriptionPaymentGenerationResponse generateNextPayment(Long subscriptionId) {
        tenantPermissionService.requireEditor();
        Long tenantId = TenantContext.getRequiredTenantId();
        Subscription subscription = subscriptionRepository.findByIdAndTenantId(subscriptionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        LocalDate scheduledBillingDate = subscription.getNextBillingDate();
        var existingPayment = subscriptionPaymentRepository.findByTenantIdAndSubscriptionIdAndScheduledBillingDate(
                tenantId,
                subscriptionId,
                scheduledBillingDate
        );
        if (existingPayment.isPresent()) {
            return new SubscriptionPaymentGenerationResponse(
                    false,
                    true,
                    "Payment already exists for the next scheduled billing date",
                    existingPayment.get().getId(),
                    existingPayment.get().getExpense().getId(),
                    scheduledBillingDate
            );
        }

        // Force manual generation for the next billing slot while preserving duplicate protection.
        boolean generated = subscriptionGenerationService.generateDueOccurrence(
                tenantId,
                subscriptionId,
                scheduledBillingDate
        );

        var createdPayment = subscriptionPaymentRepository.findByTenantIdAndSubscriptionIdAndScheduledBillingDate(
                tenantId,
                subscriptionId,
                scheduledBillingDate
        );

        if (generated && createdPayment.isPresent()) {
            return new SubscriptionPaymentGenerationResponse(
                    true,
                    false,
                    "Subscription payment generated successfully",
                    createdPayment.get().getId(),
                    createdPayment.get().getExpense().getId(),
                    scheduledBillingDate
            );
        }

        String message;
        if (createdPayment.isPresent()) {
            message = "Payment already exists for the next scheduled billing date";
        } else {
            LocalDate businessDate = LocalDate.now();
            message = "Subscription is not active for generation on " + businessDate;
        }

        return new SubscriptionPaymentGenerationResponse(
                false,
                createdPayment.isPresent(),
                message,
                createdPayment.map(SubscriptionPayment::getId).orElse(null),
                createdPayment.map(payment -> payment.getExpense().getId()).orElse(null),
                scheduledBillingDate
        );
    }

    @Transactional
    public SubscriptionPaymentGenerationResponse backfillPayment(Long subscriptionId, SubscriptionPaymentBackfillRequest request) {
        tenantPermissionService.requireEditor();
        Long tenantId = TenantContext.getRequiredTenantId();
        Subscription subscription = subscriptionRepository.findByIdAndTenantId(subscriptionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        LocalDate scheduledBillingDate = request.scheduledBillingDate();
        LocalDate today = LocalDate.now();
        if (scheduledBillingDate.isAfter(today)) {
            throw new BadRequestException("scheduledBillingDate must be today or in the past");
        }

        var existingPayment = subscriptionPaymentRepository.findByTenantIdAndSubscriptionIdAndScheduledBillingDate(
                tenantId,
                subscriptionId,
                scheduledBillingDate
        );
        if (existingPayment.isPresent()) {
            return new SubscriptionPaymentGenerationResponse(
                    false,
                    true,
                    "Payment already exists for the provided billing date",
                    existingPayment.get().getId(),
                    existingPayment.get().getExpense().getId(),
                    scheduledBillingDate
            );
        }

        boolean generated = subscriptionGenerationService.generateBackfillOccurrence(
                tenantId,
                subscription.getId(),
                scheduledBillingDate,
                request.paidDate()
        );

        var createdPayment = subscriptionPaymentRepository.findByTenantIdAndSubscriptionIdAndScheduledBillingDate(
                tenantId,
                subscriptionId,
                scheduledBillingDate
        );

        if (generated && createdPayment.isPresent()) {
            return new SubscriptionPaymentGenerationResponse(
                    true,
                    false,
                    "Subscription backfill payment created successfully",
                    createdPayment.get().getId(),
                    createdPayment.get().getExpense().getId(),
                    scheduledBillingDate
            );
        }

        return new SubscriptionPaymentGenerationResponse(
                false,
                createdPayment.isPresent(),
                createdPayment.isPresent()
                        ? "Payment already exists for the provided billing date"
                        : "Backfill payment could not be created",
                createdPayment.map(SubscriptionPayment::getId).orElse(null),
                createdPayment.map(payment -> payment.getExpense().getId()).orElse(null),
                scheduledBillingDate
        );
    }

    private SubscriptionPaymentResponse toResponse(SubscriptionPayment payment) {
        return new SubscriptionPaymentResponse(
                payment.getId(),
                payment.getSubscription().getId(),
                payment.getScheduledBillingDate(),
                payment.getExpense().getId(),
                payment.getExpense().getAmount(),
                payment.getExpense().getCurrency(),
                payment.getExpense().getStatus(),
                payment.getExpense().getDueDate(),
                payment.getExpense().getPaidDate(),
                payment.getCreatedAt()
        );
    }
}

