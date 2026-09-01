package com.elmeftouhi.expense.scheduler;

import com.elmeftouhi.expense.expense.Expense;
import com.elmeftouhi.expense.expense.ExpenseRepository;
import com.elmeftouhi.expense.expense.ExpenseStatus;
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
public class OverdueExpenseNotificationScheduler {

    private final ExpenseRepository expenseRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * *") // Daily at 8:00 AM
    @Transactional
    public void sendOverdueNotifications() {
        log.info("Starting overdue expense notification task");

        try {
            LocalDate today = LocalDate.now();

            // Note: This would need proper multi-tenant iteration in production
            // For now, this is a placeholder that processes with null tenant context
            List<Expense> overdueExpenses = expenseRepository
                    .findByTenantIdAndStatusAndDueDateBeforeAndDeletedFalse(
                            null, ExpenseStatus.PENDING, today);

            for (Expense expense : overdueExpenses) {
                try {
                    // Update status if not already overdue
                    if (expense.getStatus() != ExpenseStatus.OVERDUE) {
                        expense.setStatus(ExpenseStatus.OVERDUE);
                        expenseRepository.save(expense);
                    }

                    // Send notification
                    notificationService.sendExpenseNotification(
                            expense,
                            NotificationType.EXPENSE_OVERDUE,
                            "Your expense '" + expense.getDescription() + "' is overdue. " +
                                    "Due date was " + expense.getDueDate() + ". " +
                                    "Amount: " + expense.getAmount() + " " + expense.getCurrency()
                    );

                    log.debug("Sent overdue notification for expense {}", expense.getId());

                } catch (Exception e) {
                    log.error("Error processing overdue expense {}", expense.getId(), e);
                }
            }

            log.info("Overdue expense notification task completed. Processed {} expenses", overdueExpenses.size());

        } catch (Exception e) {
            log.error("Error in overdue expense notification scheduler", e);
        }
    }
}

