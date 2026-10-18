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
public class UpcomingExpenseNotificationScheduler {

    private final ExpenseRepository expenseRepository;
    private final NotificationService notificationService;
    private final TenantRepository tenantRepository;
    private final TenantConfigurationService tenantConfigurationService;

    @Scheduled(cron = "0 0 9 * * *") // Daily at 9:00 AM
    public void sendUpcomingExpenseNotifications() {
        log.info("Starting upcoming expense notification task");

        List<Tenant> tenants = tenantRepository.findAll();
        for (Tenant tenant : tenants) {
            processTenant(tenant.getId());
        }

        log.info("Upcoming expense notification task completed");
    }

    @Transactional
    public void processTenant(Long tenantId) {
        TenantConfiguration tenantConfiguration = tenantConfigurationService.loadConfiguration(tenantId);
        TenantContext.setTenantId(tenantId);
        ConfigurationContext.setConfiguration(tenantConfiguration);
        try {
            LocalDate today = LocalDate.now(ZoneId.of(tenantConfiguration.getTimezone()));
            LocalDate tomorrow = today.plusDays(1);
            List<Expense> upcomingExpenses = expenseRepository
                    .findByTenantIdAndStatusAndDueDateBetweenAndDeletedFalseAndSubscriptionTemplateFalse(
                            tenantId,
                            ExpenseStatus.PENDING,
                            today,
                            tomorrow);

            for (Expense expense : upcomingExpenses) {
                notificationService.sendExpenseNotification(
                        expense,
                        NotificationType.EXPENSE_DUE_SOON,
                        "Reminder: Your expense '" + expense.getDescription() + "' is due tomorrow. " +
                                "Amount: " + expense.getAmount() + " " + expense.getCurrency()
                );
            }
            log.info("Processed {} upcoming expense reminders for tenant {}", upcomingExpenses.size(), tenantId);
        } catch (Exception ex) {
            log.error("Error while processing upcoming reminders for tenant {}", tenantId, ex);
        } finally {
            ConfigurationContext.clear();
            TenantContext.clear();
        }
    }
}

