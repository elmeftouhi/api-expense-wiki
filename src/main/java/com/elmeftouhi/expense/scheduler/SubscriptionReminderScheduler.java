package com.elmeftouhi.expense.scheduler;

import com.elmeftouhi.expense.expense.SubscriptionReminderService;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.tenant.Tenant;
import com.elmeftouhi.expense.tenant.TenantRepository;
import com.elmeftouhi.expense.tenantconfig.ConfigurationContext;
import com.elmeftouhi.expense.tenantconfig.TenantConfiguration;
import com.elmeftouhi.expense.tenantconfig.TenantConfigurationService;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionReminderScheduler {

    private final TenantRepository tenantRepository;
    private final TenantConfigurationService tenantConfigurationService;
    private final SubscriptionReminderService subscriptionReminderService;

    @Scheduled(cron = "0 0 8 * * *")
    public void sendSubscriptionReminders() {
        for (Tenant tenant : tenantRepository.findAll()) {
            processTenant(tenant.getId());
        }
    }

    @Transactional
    public void processTenant(Long tenantId) {
        TenantConfiguration configuration = tenantConfigurationService.loadConfiguration(tenantId);
        TenantContext.setTenantId(tenantId);
        ConfigurationContext.setConfiguration(configuration);
        try {
            LocalDate businessDate = LocalDate.now(ZoneId.of(configuration.getTimezone()));
            int remindersSent = subscriptionReminderService.sendRemindersForTenant(tenantId, businessDate);
            log.info("Subscription reminders sent for tenant {}: {}", tenantId, remindersSent);
        } catch (Exception ex) {
            log.error("Failed to send subscription reminders for tenant {}", tenantId, ex);
        } finally {
            ConfigurationContext.clear();
            TenantContext.clear();
        }
    }
}

