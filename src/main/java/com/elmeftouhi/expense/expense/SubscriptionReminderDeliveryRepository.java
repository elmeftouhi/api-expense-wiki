package com.elmeftouhi.expense.expense;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionReminderDeliveryRepository extends JpaRepository<SubscriptionReminderDelivery, Long> {

    boolean existsByTenantIdAndSubscriptionIdAndScheduledBillingDateAndUserIdAndChannel(
            Long tenantId,
            Long subscriptionId,
            LocalDate scheduledBillingDate,
            Long userId,
            String channel);
}

