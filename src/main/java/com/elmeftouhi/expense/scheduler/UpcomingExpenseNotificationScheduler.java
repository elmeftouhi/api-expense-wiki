package com.elmeftouhi.expense.scheduler;

import com.elmeftouhi.expense.expense.Expense;
import com.elmeftouhi.expense.expense.ExpenseRepository;
import com.elmeftouhi.expense.notification.NotificationService;
import com.elmeftouhi.expense.notification.NotificationType;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpcomingExpenseNotificationScheduler {

    private final ExpenseRepository expenseRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * *") // Daily at 9:00 AM
    @Transactional
    public void sendUpcomingExpenseNotifications() {
        log.info("Starting upcoming expense notification task");

        try {
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);

            // Note: This would need proper multi-tenant iteration in production
            // For now, this is a placeholder that needs tenant context
            List<Expense> upcomingExpenses = expenseRepository
                    .findByTenantIdAndDueDateBetweenAndDeletedFalse(null, today, tomorrow);

            for (Expense expense : upcomingExpenses) {
                try {
                    // Send notification
                    notificationService.sendExpenseNotification(
                            expense,
                            NotificationType.EXPENSE_DUE_SOON,
                            "Reminder: Your expense '" + expense.getDescription() + "' is due tomorrow. " +
                                    "Amount: " + expense.getAmount() + " " + expense.getCurrency()
                    );

                    log.debug("Sent upcoming notification for expense {}", expense.getId());

                } catch (Exception e) {
                    log.error("Error processing upcoming expense {}", expense.getId(), e);
                }
            }

            log.info("Upcoming expense notification task completed. Processed {} expenses", upcomingExpenses.size());

        } catch (Exception e) {
            log.error("Error in upcoming expense notification scheduler", e);
        }
    }
}

