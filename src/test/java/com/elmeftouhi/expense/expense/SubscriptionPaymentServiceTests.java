package com.elmeftouhi.expense.expense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elmeftouhi.expense.expense.dto.SubscriptionPaymentGenerationResponse;
import com.elmeftouhi.expense.expense.dto.SubscriptionPaymentBackfillRequest;
import com.elmeftouhi.expense.expense.entity.Expense;
import com.elmeftouhi.expense.expense.entity.Subscription;
import com.elmeftouhi.expense.expense.entity.SubscriptionPayment;
import com.elmeftouhi.expense.expense.repository.SubscriptionPaymentRepository;
import com.elmeftouhi.expense.expense.repository.SubscriptionRepository;
import com.elmeftouhi.expense.expense.service.SubscriptionGenerationService;
import com.elmeftouhi.expense.expense.service.SubscriptionPaymentService;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionPaymentServiceTests {

    @Mock
    private SubscriptionPaymentRepository subscriptionPaymentRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionGenerationService subscriptionGenerationService;

    @InjectMocks
    private SubscriptionPaymentService subscriptionPaymentService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void generateNextPayment_returnsAlreadyExistsWhenPaymentWasAlreadyCreated() {
        Long tenantId = 1L;
        Long subscriptionId = 10L;
        LocalDate scheduledDate = LocalDate.of(2026, 11, 1);
        TenantContext.setTenantId(tenantId);

        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.assignTenantId(tenantId);
        subscription.setNextBillingDate(scheduledDate);

        SubscriptionPayment existingPayment = payment(500L, subscription, 700L, scheduledDate);

        when(subscriptionRepository.findByIdAndTenantId(subscriptionId, tenantId)).thenReturn(Optional.of(subscription));
        when(subscriptionPaymentRepository.findByTenantIdAndSubscriptionIdAndScheduledBillingDate(
                tenantId, subscriptionId, scheduledDate)).thenReturn(Optional.of(existingPayment));

        SubscriptionPaymentGenerationResponse response = subscriptionPaymentService.generateNextPayment(subscriptionId);

        assertTrue(response.alreadyExists());
        assertEquals(false, response.generated());
        assertEquals(500L, response.paymentId());
        assertEquals(700L, response.expenseId());
        verify(subscriptionGenerationService, never()).generateDueOccurrence(tenantId, subscriptionId, scheduledDate);
    }

    @Test
    void generateNextPayment_generatesAndReturnsCreatedPayment() {
        Long tenantId = 1L;
        Long subscriptionId = 10L;
        LocalDate scheduledDate = LocalDate.of(2026, 11, 1);
        TenantContext.setTenantId(tenantId);

        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.assignTenantId(tenantId);
        subscription.setNextBillingDate(scheduledDate);

        SubscriptionPayment createdPayment = payment(501L, subscription, 701L, scheduledDate);

        when(subscriptionRepository.findByIdAndTenantId(subscriptionId, tenantId)).thenReturn(Optional.of(subscription));
        when(subscriptionPaymentRepository.findByTenantIdAndSubscriptionIdAndScheduledBillingDate(
                tenantId, subscriptionId, scheduledDate)).thenReturn(Optional.empty(), Optional.of(createdPayment));
        when(subscriptionGenerationService.generateDueOccurrence(tenantId, subscriptionId, scheduledDate)).thenReturn(true);

        SubscriptionPaymentGenerationResponse response = subscriptionPaymentService.generateNextPayment(subscriptionId);

        assertTrue(response.generated());
        assertEquals(false, response.alreadyExists());
        assertEquals(501L, response.paymentId());
        assertEquals(701L, response.expenseId());
        verify(subscriptionGenerationService).generateDueOccurrence(tenantId, subscriptionId, scheduledDate);
    }

    @Test
    void backfillPayment_returnsAlreadyExistsWhenSameBillingDateAlreadyCreated() {
        Long tenantId = 1L;
        Long subscriptionId = 10L;
        LocalDate scheduledDate = LocalDate.now().minusDays(1);
        TenantContext.setTenantId(tenantId);

        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.assignTenantId(tenantId);
        subscription.setNextBillingDate(LocalDate.of(2026, 11, 1));

        SubscriptionPayment existingPayment = payment(502L, subscription, 702L, scheduledDate);

        when(subscriptionRepository.findByIdAndTenantId(subscriptionId, tenantId)).thenReturn(Optional.of(subscription));
        when(subscriptionPaymentRepository.findByTenantIdAndSubscriptionIdAndScheduledBillingDate(
                tenantId, subscriptionId, scheduledDate)).thenReturn(Optional.of(existingPayment));

        SubscriptionPaymentGenerationResponse response = subscriptionPaymentService.backfillPayment(
                subscriptionId,
                new SubscriptionPaymentBackfillRequest(scheduledDate, scheduledDate));

        assertTrue(response.alreadyExists());
        assertEquals(false, response.generated());
        verify(subscriptionGenerationService, never()).generateBackfillOccurrence(tenantId, subscriptionId, scheduledDate, scheduledDate);
    }

    @Test
    void backfillPayment_rejectsFutureBillingDate() {
        Long tenantId = 1L;
        Long subscriptionId = 10L;
        LocalDate futureDate = LocalDate.now().plusDays(5);
        TenantContext.setTenantId(tenantId);

        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.assignTenantId(tenantId);
        when(subscriptionRepository.findByIdAndTenantId(subscriptionId, tenantId)).thenReturn(Optional.of(subscription));

        assertThrows(
                BadRequestException.class,
                () -> subscriptionPaymentService.backfillPayment(
                        subscriptionId,
                        new SubscriptionPaymentBackfillRequest(futureDate, futureDate)));
    }

    private SubscriptionPayment payment(Long paymentId, Subscription subscription, Long expenseId, LocalDate scheduledDate) {
        Expense expense = new Expense();
        expense.setId(expenseId);
        expense.setAmount(BigDecimal.valueOf(12.34));

        SubscriptionPayment payment = new SubscriptionPayment();
        payment.setId(paymentId);
        payment.assignTenantId(subscription.getTenantId());
        payment.setSubscription(subscription);
        payment.setExpense(expense);
        payment.setScheduledBillingDate(scheduledDate);
        return payment;
    }
}

