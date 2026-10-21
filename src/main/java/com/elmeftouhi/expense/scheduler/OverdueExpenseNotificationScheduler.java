package com.elmeftouhi.expense.scheduler;

import com.elmeftouhi.expense.expense.entity.Expense;
import com.elmeftouhi.expense.expense.repository.ExpenseRepository;
import com.elmeftouhi.expense.expense.ExpenseStatus;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.notification.service.NotificationService;
import com.elmeftouhi.expense.notification.NotificationType;
import com.elmeftouhi.expense.tenant.entity.Tenant;
import com.elmeftouhi.expense.tenant.repository.TenantRepository;
import com.elmeftouhi.expense.tenantconfig.ConfigurationContext;
import com.elmeftouhi.expense.tenantconfig.entity.TenantConfiguration;
import com.elmeftouhi.expense.tenantconfig.service.TenantConfigurationService;
import java.time.LocalDate;
import java.time.ZoneId;
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
    private final TenantRepository tenantRepository;
    private final TenantConfigurationService tenantConfigurationService;

    @Scheduled(cron = "0 0 8 * * *") // Daily at 8:00 AM
    public void sendOverdueNotifications() {
        log.info("Starting overdue expense notification task");
        for (Tenant tenant : tenantRepository.findAll()) {
            processTenant(tenant.getId());
        }

        log.info("Overdue expense notification task completed");
    }

    @Transactional
    public void processTenant(Long tenantId) {
        TenantConfiguration configuration = tenantConfigurationService.loadConfiguration(tenantId);
        TenantContext.setTenantId(tenantId);
        ConfigurationContext.setConfiguration(configuration);
        try {
            LocalDate today = LocalDate.now(ZoneId.of(configuration.getTimezone()));
            List<Expense> overdueExpenses = expenseRepository
                    .findByTenantIdAndStatusAndDueDateBeforeAndDeletedFalse(
                            tenantId,
                            ExpenseStatus.PENDING,
                            today);

            for (Expense expense : overdueExpenses) {
                if (expense.getStatus() != ExpenseStatus.OVERDUE) {
                    expense.setStatus(ExpenseStatus.OVERDUE);
                    expenseRepository.save(expense);
                }

                notificationService.sendExpenseNotification(
                        expense,
                        NotificationType.EXPENSE_OVERDUE,
                        "Your expense '" + expense.getDescription() + "' is overdue. " +
                                "Due date was " + expense.getDueDate() + ". " +
                                "Amount: " + expense.getAmount() + " " + expense.getCurrency()
                );
            }

            log.info("Processed {} overdue expenses for tenant {}", overdueExpenses.size(), tenantId);
        } catch (Exception ex) {
            log.error("Error in overdue scheduler for tenant {}", tenantId, ex);
        } finally {
            ConfigurationContext.clear();
            TenantContext.clear();
        }
    }
}

