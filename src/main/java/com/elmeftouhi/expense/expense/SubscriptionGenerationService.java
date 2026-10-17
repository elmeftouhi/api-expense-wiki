package com.elmeftouhi.expense.expense;

import com.elmeftouhi.expense.notification.NotificationService;
import com.elmeftouhi.expense.notification.NotificationType;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionGenerationService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionOccurrenceRepository occurrenceRepository;
    private final ExpenseService expenseService;
    private final NotificationService notificationService;

    @Transactional
    public boolean generateDueOccurrence(Long tenantId, Long subscriptionId, LocalDate businessDate) {
        Subscription subscription = subscriptionRepository.findForUpdate(subscriptionId, tenantId)
                .orElse(null);
        if (subscription == null) {
            return false;
        }

        if (!subscription.isActiveFor(businessDate)) {
            if (subscription.getStatus() == SubscriptionLifecycleStatus.ACTIVE
                    && subscription.getEndDate() != null
                    && subscription.getNextBillingDate().isAfter(subscription.getEndDate())) {
                subscription.setStatus(SubscriptionLifecycleStatus.ENDED);
                subscription.setUpdatedAt(Instant.now());
                subscriptionRepository.save(subscription);
            }
            return false;
        }

        LocalDate scheduledBillingDate = subscription.getNextBillingDate();

        if (occurrenceRepository.existsByTenantIdAndSubscriptionIdAndScheduledBillingDate(
                tenantId,
                subscriptionId,
                scheduledBillingDate)) {
            advanceSubscription(subscription);
            return false;
        }

        try {
            Expense generatedExpense = expenseService.createGeneratedExpenseFromSubscription(subscription, scheduledBillingDate);

            SubscriptionOccurrence occurrence = new SubscriptionOccurrence();
            occurrence.assignTenantId(tenantId);
            occurrence.setSubscription(subscription);
            occurrence.setGeneratedExpense(generatedExpense);
            occurrence.setScheduledBillingDate(scheduledBillingDate);
            occurrenceRepository.save(occurrence);

            advanceSubscription(subscription);

            notificationService.sendExpenseNotification(
                    generatedExpense,
                    NotificationType.SUBSCRIPTION_NEXT,
                    "Subscription bill generated for '" + subscription.getName() + "' on "
                            + scheduledBillingDate + " (" + generatedExpense.getAmount() + " "
                            + generatedExpense.getCurrency() + ")"
            );
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            log.info("Duplicate subscription occurrence suppressed for subscription {} on {}", subscriptionId, scheduledBillingDate);
            advanceSubscription(subscription);
            return false;
        }
    }

    private void advanceSubscription(Subscription subscription) {
        LocalDate nextDate = subscription.calculateNextBillingDate();
        subscription.setNextBillingDate(nextDate);
        if (subscription.getEndDate() != null && nextDate.isAfter(subscription.getEndDate())) {
            subscription.setStatus(SubscriptionLifecycleStatus.ENDED);
        }
        subscription.setUpdatedAt(Instant.now());
        subscriptionRepository.save(subscription);
    }
}

