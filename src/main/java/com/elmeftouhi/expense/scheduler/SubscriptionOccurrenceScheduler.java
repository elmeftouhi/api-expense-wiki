package com.elmeftouhi.expense.scheduler;

import com.elmeftouhi.expense.expense.Subscription;
import com.elmeftouhi.expense.expense.SubscriptionGenerationService;
import com.elmeftouhi.expense.expense.SubscriptionRepository;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.tenant.Tenant;
import com.elmeftouhi.expense.tenant.TenantRepository;
import com.elmeftouhi.expense.tenantconfig.ConfigurationContext;
import com.elmeftouhi.expense.tenantconfig.TenantConfiguration;
import com.elmeftouhi.expense.tenantconfig.TenantConfigurationService;
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
public class SubscriptionOccurrenceScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionGenerationService subscriptionGenerationService;
    private final TenantRepository tenantRepository;
    private final TenantConfigurationService tenantConfigurationService;

    @Scheduled(cron = "0 0 0 * * *") // Daily at 12:00 AM
    public void generateSubscriptionOccurrences() {
        log.info("Starting subscription occurrence generation");

        List<Tenant> tenants = tenantRepository.findAll();
        for (Tenant tenant : tenants) {
            processTenantSubscriptions(tenant.getId());
        }

        log.info("Subscription occurrence generation completed successfully");
    }

    @Transactional
    public void processTenantSubscriptions(Long tenantId) {
        TenantConfiguration tenantConfiguration = tenantConfigurationService.loadConfiguration(tenantId);
        TenantContext.setTenantId(tenantId);
        ConfigurationContext.setConfiguration(tenantConfiguration);

        try {
            LocalDate businessDate = LocalDate.now(ZoneId.of(tenantConfiguration.getTimezone()));
            List<Subscription> dueSubscriptions = subscriptionRepository.findDueForGeneration(tenantId, businessDate);
            int generated = 0;
            for (Subscription subscription : dueSubscriptions) {
                if (subscriptionGenerationService.generateDueOccurrence(tenantId, subscription.getId(), businessDate)) {
                    generated++;
                }
            }
            log.info(
                    "Processed {} due subscriptions for tenant {} on business date {} (generated={})",
                    dueSubscriptions.size(),
                    tenantId,
                    businessDate,
                    generated
            );
        } catch (Exception ex) {
            log.error("Failed to process subscription occurrences for tenant {}", tenantId, ex);
        } finally {
            ConfigurationContext.clear();
            TenantContext.clear();
        }
    }
}

