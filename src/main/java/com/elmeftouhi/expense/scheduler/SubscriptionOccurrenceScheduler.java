package com.elmeftouhi.expense.scheduler;

import com.elmeftouhi.expense.expense.Expense;
import com.elmeftouhi.expense.expense.ExpenseRepository;
import com.elmeftouhi.expense.expense.ExpenseStatus;
import com.elmeftouhi.expense.expense.ExpenseSubscription;
import com.elmeftouhi.expense.expense.ExpenseSubscriptionRepository;
import com.elmeftouhi.expense.expense.SubscriptionOccurrence;
import com.elmeftouhi.expense.expense.SubscriptionOccurrenceRepository;
import com.elmeftouhi.expense.notification.NotificationService;
import com.elmeftouhi.expense.notification.NotificationType;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionOccurrenceScheduler {

    private final ExpenseSubscriptionRepository subscriptionRepository;
    private final ExpenseRepository expenseRepository;
    private final SubscriptionOccurrenceRepository occurrenceRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 0 * * *") // Daily at 12:00 AM
    @Transactional
    public void generateSubscriptionOccurrences() {
        log.info("Starting subscription occurrence generation");
        LocalDate today = LocalDate.now();

        try {
            // Find all subscriptions that need new occurrences
            // We need to find all active subscriptions with tenants
            // Since we don't have a multi-tenant context here, we'll process all

            // This would need to be implemented with proper multi-tenant support
            // For now, we'll create a helper method
            processAllSubscriptions(today);

            log.info("Subscription occurrence generation completed successfully");
        } catch (Exception e) {
            log.error("Error generating subscription occurrences", e);
        }
    }

    @Transactional
    public void processSubscriptionForDate(ExpenseSubscription subscription, LocalDate today) {
        if (!subscription.shouldGenerateNextOccurrence(today)) {
            return;
        }

        try {
            // Check if occurrence already exists for this date
            if (occurrenceRepository.existsBySubscriptionIdAndOccurrenceDate(
                    subscription.getId(), subscription.getNextOccurrenceDate())) {
                log.debug("Occurrence already exists for subscription {} on {}",
                        subscription.getId(), subscription.getNextOccurrenceDate());
                return;
            }

            // Create new expense from the original
            Expense originalExpense = subscription.getExpense();
            Expense newExpense = new Expense();
            newExpense.assignTenantId(originalExpense.getTenantId());
            newExpense.setCategory(originalExpense.getCategory());
            newExpense.setUserId(originalExpense.getUserId());
            newExpense.setDescription(originalExpense.getDescription());
            newExpense.setAmount(originalExpense.getAmount());
            newExpense.setCurrency(originalExpense.getCurrency());
            newExpense.setType(originalExpense.getType());
            newExpense.setStatus(ExpenseStatus.PENDING);
            newExpense.setDueDate(subscription.getNextOccurrenceDate());
            newExpense.setPaymentMethod(originalExpense.getPaymentMethod());
            newExpense.setNotes("Auto-generated from subscription #" + subscription.getId());

            Expense savedExpense = expenseRepository.save(newExpense);

            // Record the occurrence
            SubscriptionOccurrence occurrence = new SubscriptionOccurrence();
            occurrence.assignTenantId(subscription.getTenantId());
            occurrence.setSubscription(subscription);
            occurrence.setGeneratedExpense(savedExpense);
            occurrence.setOccurrenceDate(subscription.getNextOccurrenceDate());

            occurrenceRepository.save(occurrence);

            // Update subscription's next occurrence date
            LocalDate nextDate = subscription.calculateNextOccurrenceDate();
            subscription.setNextOccurrenceDate(nextDate);
            subscription.setOccurrenceCount(subscription.getOccurrenceCount() + 1);
            subscriptionRepository.save(subscription);

            // Send notification
            notificationService.sendExpenseNotification(
                    newExpense,
                    NotificationType.SUBSCRIPTION_NEXT,
                    "New expense generated from your subscription: " + originalExpense.getDescription() +
                            " - Amount: " + newExpense.getAmount() + " " + newExpense.getCurrency()
            );

            log.info("Generated occurrence for subscription {} with due date {}",
                    subscription.getId(), subscription.getNextOccurrenceDate());

        } catch (Exception e) {
            log.error("Error processing subscription {}", subscription.getId(), e);
        }
    }

    private void processAllSubscriptions(LocalDate today) {
        // This is a placeholder - in production, you'd need proper multi-tenant iteration
        // For now, we're just logging that this would be implemented
        log.debug("Processing all active subscriptions for date {}", today);
    }
}

