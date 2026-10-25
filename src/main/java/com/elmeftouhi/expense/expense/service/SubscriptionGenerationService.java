package com.elmeftouhi.expense.expense.service;

import com.elmeftouhi.expense.expense.SubscriptionLifecycleStatus;
import com.elmeftouhi.expense.expense.entity.Expense;
import com.elmeftouhi.expense.expense.entity.SubscriptionPayment;
import com.elmeftouhi.expense.expense.entity.Subscription;
import com.elmeftouhi.expense.expense.repository.SubscriptionRepository;
import com.elmeftouhi.expense.expense.repository.SubscriptionPaymentRepository;
import com.elmeftouhi.expense.notification.service.NotificationService;
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
    private final SubscriptionPaymentRepository paymentRepository;
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

        if (paymentRepository.existsByTenantIdAndSubscriptionIdAndScheduledBillingDate(
                tenantId,
                subscriptionId,
                scheduledBillingDate)) {
            log.info(
                    "Payment already generated for subscription {} on {}. Skipping to avoid duplicates.",
                    subscriptionId,
                    scheduledBillingDate
            );
            advanceSubscription(subscription);
            return false;
        }

        try {
            Expense generatedExpense = expenseService.createGeneratedExpenseFromSubscription(subscription, scheduledBillingDate);

            SubscriptionPayment payment = new SubscriptionPayment();
            payment.assignTenantId(tenantId);
            payment.setSubscription(subscription);
            payment.setExpense(generatedExpense);
            payment.setScheduledBillingDate(scheduledBillingDate);
            paymentRepository.save(payment);

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
            log.info("Duplicate subscription payment suppressed for subscription {} on {}", subscriptionId, scheduledBillingDate);
            advanceSubscription(subscription);
            return false;
        }
    }

    @Transactional
    public boolean generateBackfillOccurrence(
            Long tenantId,
            Long subscriptionId,
            LocalDate scheduledBillingDate,
            LocalDate paidDate) {
        Subscription subscription = subscriptionRepository.findForUpdate(subscriptionId, tenantId)
                .orElse(null);
        if (subscription == null) {
            return false;
        }

        if (paymentRepository.existsByTenantIdAndSubscriptionIdAndScheduledBillingDate(
                tenantId,
                subscriptionId,
                scheduledBillingDate)) {
            return false;
        }

        try {
            Expense generatedExpense = expenseService.createGeneratedExpenseFromSubscription(
                    subscription,
                    scheduledBillingDate,
                    paidDate);

            SubscriptionPayment payment = new SubscriptionPayment();
            payment.assignTenantId(tenantId);
            payment.setSubscription(subscription);
            payment.setExpense(generatedExpense);
            payment.setScheduledBillingDate(scheduledBillingDate);
            paymentRepository.save(payment);

            advanceSubscriptionAfterDate(subscription, scheduledBillingDate);
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            log.info("Duplicate backfill payment suppressed for subscription {} on {}", subscriptionId, scheduledBillingDate);
            advanceSubscriptionAfterDate(subscription, scheduledBillingDate);
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

    private void advanceSubscriptionAfterDate(Subscription subscription, LocalDate paidOccurrenceDate) {
        while (!subscription.getNextBillingDate().isAfter(paidOccurrenceDate)) {
            subscription.setNextBillingDate(subscription.calculateNextBillingDate());
        }
        if (subscription.getEndDate() != null && subscription.getNextBillingDate().isAfter(subscription.getEndDate())) {
            subscription.setStatus(SubscriptionLifecycleStatus.ENDED);
        }
        subscription.setUpdatedAt(Instant.now());
        subscriptionRepository.save(subscription);
    }
}

