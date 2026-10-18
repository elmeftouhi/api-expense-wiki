package com.elmeftouhi.expense.expense.service;

import com.elmeftouhi.expense.expense.SubscriptionLifecycleStatus;
import com.elmeftouhi.expense.expense.entity.SubscriptionReminderDelivery;
import com.elmeftouhi.expense.expense.entity.Subscription;
import com.elmeftouhi.expense.expense.repository.SubscriptionRepository;
import com.elmeftouhi.expense.expense.repository.SubscriptionReminderDeliveryRepository;
import com.elmeftouhi.expense.notification.service.NotificationService;
import com.elmeftouhi.expense.notification.NotificationType;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionReminderService {

    private static final int MAX_REMINDER_LOOKAHEAD_DAYS = 90;
    private static final String CHANNEL_IN_APP = "IN_APP";

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionReminderDeliveryRepository reminderDeliveryRepository;
    private final NotificationService notificationService;

    @Transactional
    public int sendRemindersForTenant(Long tenantId, LocalDate businessDate) {
        List<Subscription> candidates = subscriptionRepository.findUpcomingForReminderWindow(
                tenantId,
                businessDate,
                businessDate.plusDays(MAX_REMINDER_LOOKAHEAD_DAYS)
        );

        int sent = 0;
        for (Subscription subscription : candidates) {
            if (!isReminderDueToday(subscription, businessDate)) {
                continue;
            }

            if (sendReminderIfNotSent(tenantId, subscription)) {
                sent++;
            }
        }
        return sent;
    }

    private boolean isReminderDueToday(Subscription subscription, LocalDate businessDate) {
        if (subscription.getStatus() != SubscriptionLifecycleStatus.ACTIVE) {
            return false;
        }

        LocalDate dueDate = subscription.getNextBillingDate();
        if (!businessDate.isBefore(dueDate)) {
            return false;
        }

        LocalDate reminderDate = dueDate.minusDays(subscription.getReminderLeadDays());
        return !businessDate.isBefore(reminderDate);
    }

    private boolean sendReminderIfNotSent(Long tenantId, Subscription subscription) {
        LocalDate scheduledBillingDate = subscription.getNextBillingDate();
        Long userId = subscription.getOwnerUserId();

        if (reminderDeliveryRepository.existsByTenantIdAndSubscriptionIdAndScheduledBillingDateAndUserIdAndChannel(
                tenantId,
                subscription.getId(),
                scheduledBillingDate,
                userId,
                CHANNEL_IN_APP)) {
            return false;
        }

        try {
            SubscriptionReminderDelivery delivery = new SubscriptionReminderDelivery();
            delivery.assignTenantId(tenantId);
            delivery.setSubscription(subscription);
            delivery.setScheduledBillingDate(scheduledBillingDate);
            delivery.setUserId(userId);
            delivery.setChannel(CHANNEL_IN_APP);
            reminderDeliveryRepository.save(delivery);

            notificationService.sendNotification(
                    tenantId,
                    userId,
                    NotificationType.PAYMENT_REMINDER,
                    "Subscription reminder",
                    "Upcoming subscription '" + subscription.getName() + "' will be billed on " + scheduledBillingDate
                            + " (" + subscription.getAmount() + " " + subscription.getCurrency() + ")",
                    "SUBSCRIPTION",
                    subscription.getId()
            );
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            log.info(
                    "Duplicate subscription reminder suppressed for subscription {} billing date {}",
                    subscription.getId(),
                    scheduledBillingDate
            );
            return false;
        }
    }
}


