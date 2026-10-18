package com.elmeftouhi.expense.expense;

import com.elmeftouhi.expense.expense.entity.Expense;
import com.elmeftouhi.expense.expense.entity.SubscriptionOccurrence;
import com.elmeftouhi.expense.expense.repository.SubscriptionRepository;
import com.elmeftouhi.expense.expense.repository.SubscriptionOccurrenceRepository;
import com.elmeftouhi.expense.expense.service.ExpenseService;
import com.elmeftouhi.expense.expense.service.SubscriptionGenerationService;
import com.elmeftouhi.expense.expense.entity.Subscription;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elmeftouhi.expense.category.entity.Category;
import com.elmeftouhi.expense.notification.service.NotificationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionGenerationServiceTests {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionOccurrenceRepository occurrenceRepository;

    @Mock
    private ExpenseService expenseService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SubscriptionGenerationService subscriptionGenerationService;

    @Test
    void generateDueOccurrence_createsPendingExpenseAndOccurrence() {
        Long tenantId = 1L;
        Long subscriptionId = 10L;
        LocalDate businessDate = LocalDate.of(2026, 11, 5);

        Subscription subscription = dueSubscription(tenantId, subscriptionId, businessDate);
        Expense generatedExpense = new Expense();
        generatedExpense.setId(99L);
        generatedExpense.setStatus(ExpenseStatus.PENDING);
        generatedExpense.setAmount(BigDecimal.valueOf(15));
        generatedExpense.setCurrency("USD");

        when(subscriptionRepository.findForUpdate(subscriptionId, tenantId)).thenReturn(Optional.of(subscription));
        when(occurrenceRepository.existsByTenantIdAndSubscriptionIdAndScheduledBillingDate(tenantId, subscriptionId, businessDate))
                .thenReturn(false);
        when(expenseService.createGeneratedExpenseFromSubscription(subscription, businessDate)).thenReturn(generatedExpense);

        boolean generated = subscriptionGenerationService.generateDueOccurrence(tenantId, subscriptionId, businessDate);

        assertTrue(generated);
        verify(occurrenceRepository).save(any(SubscriptionOccurrence.class));
        verify(notificationService).sendExpenseNotification(any(), any(), any());
    }

    @Test
    void generateDueOccurrence_isIdempotentWhenOccurrenceAlreadyExists() {
        Long tenantId = 1L;
        Long subscriptionId = 10L;
        LocalDate businessDate = LocalDate.of(2026, 11, 5);

        Subscription subscription = dueSubscription(tenantId, subscriptionId, businessDate);

        when(subscriptionRepository.findForUpdate(subscriptionId, tenantId)).thenReturn(Optional.of(subscription));
        when(occurrenceRepository.existsByTenantIdAndSubscriptionIdAndScheduledBillingDate(tenantId, subscriptionId, businessDate))
                .thenReturn(true);

        boolean generated = subscriptionGenerationService.generateDueOccurrence(tenantId, subscriptionId, businessDate);

        assertFalse(generated);
        verify(expenseService, never()).createGeneratedExpenseFromSubscription(any(), any());
    }

    private Subscription dueSubscription(Long tenantId, Long id, LocalDate nextBillingDate) {
        Category category = new Category();
        category.setId(1L);

        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.assignTenantId(tenantId);
        subscription.setStatus(SubscriptionLifecycleStatus.ACTIVE);
        subscription.setCategory(category);
        subscription.setOwnerUserId(7L);
        subscription.setName("Netflix");
        subscription.setAmount(BigDecimal.valueOf(15));
        subscription.setCurrency("USD");
        subscription.setFrequency(SubscriptionFrequency.MONTHLY);
        subscription.setNextBillingDate(nextBillingDate);
        subscription.setAnchorDayOfMonth(nextBillingDate.getDayOfMonth());
        subscription.setAnchorMonthOfYear(nextBillingDate.getMonthValue());
        return subscription;
    }
}

