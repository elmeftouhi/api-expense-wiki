package com.elmeftouhi.expense.expense;

import com.elmeftouhi.expense.expense.entity.SubscriptionReminderDelivery;
import com.elmeftouhi.expense.expense.repository.SubscriptionRepository;
import com.elmeftouhi.expense.expense.repository.SubscriptionReminderDeliveryRepository;
import com.elmeftouhi.expense.expense.service.SubscriptionReminderService;
import com.elmeftouhi.expense.expense.entity.Subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elmeftouhi.expense.category.entity.Category;
import com.elmeftouhi.expense.notification.service.NotificationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionReminderServiceTests {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionReminderDeliveryRepository reminderDeliveryRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SubscriptionReminderService subscriptionReminderService;

    @Test
    void sendReminders_sendsOncePerSubscriptionOccurrence() {
        Long tenantId = 1L;
        LocalDate businessDate = LocalDate.of(2026, 11, 2);
        Subscription subscription = upcomingSubscription(tenantId, 5L, LocalDate.of(2026, 11, 5), 3);

        when(subscriptionRepository.findUpcomingForReminderWindow(tenantId, businessDate, businessDate.plusDays(90)))
                .thenReturn(List.of(subscription));
        when(reminderDeliveryRepository.existsByTenantIdAndSubscriptionIdAndScheduledBillingDateAndUserIdAndChannel(
                tenantId,
                5L,
                LocalDate.of(2026, 11, 5),
                11L,
                "IN_APP"
        )).thenReturn(false);

        int sent = subscriptionReminderService.sendRemindersForTenant(tenantId, businessDate);

        assertEquals(1, sent);
        verify(reminderDeliveryRepository).save(any(SubscriptionReminderDelivery.class));
        verify(notificationService).sendNotification(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void sendReminders_deduplicatesAlreadyDeliveredReminder() {
        Long tenantId = 1L;
        LocalDate businessDate = LocalDate.of(2026, 11, 2);
        Subscription subscription = upcomingSubscription(tenantId, 5L, LocalDate.of(2026, 11, 5), 3);

        when(subscriptionRepository.findUpcomingForReminderWindow(tenantId, businessDate, businessDate.plusDays(90)))
                .thenReturn(List.of(subscription));
        when(reminderDeliveryRepository.existsByTenantIdAndSubscriptionIdAndScheduledBillingDateAndUserIdAndChannel(
                tenantId,
                5L,
                LocalDate.of(2026, 11, 5),
                11L,
                "IN_APP"
        )).thenReturn(true);

        int sent = subscriptionReminderService.sendRemindersForTenant(tenantId, businessDate);

        assertEquals(0, sent);
        verify(reminderDeliveryRepository, never()).save(any(SubscriptionReminderDelivery.class));
    }

    private Subscription upcomingSubscription(Long tenantId, Long id, LocalDate nextBillingDate, int leadDays) {
        Category category = new Category();
        category.setId(3L);

        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.assignTenantId(tenantId);
        subscription.setCategory(category);
        subscription.setName("Internet");
        subscription.setAmount(BigDecimal.valueOf(45));
        subscription.setCurrency("USD");
        subscription.setOwnerUserId(11L);
        subscription.setFrequency(SubscriptionFrequency.MONTHLY);
        subscription.setStatus(SubscriptionLifecycleStatus.ACTIVE);
        subscription.setNextBillingDate(nextBillingDate);
        subscription.setReminderLeadDays(leadDays);
        subscription.setAnchorDayOfMonth(nextBillingDate.getDayOfMonth());
        subscription.setAnchorMonthOfYear(nextBillingDate.getMonthValue());
        return subscription;
    }
}

