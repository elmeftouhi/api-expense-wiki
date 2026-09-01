package com.elmeftouhi.expense.expense;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionOccurrenceRepository extends JpaRepository<SubscriptionOccurrence, Long> {

    List<SubscriptionOccurrence> findBySubscriptionId(Long subscriptionId);

    List<SubscriptionOccurrence> findBySubscriptionIdOrderByOccurrenceDateDesc(Long subscriptionId);

    List<SubscriptionOccurrence> findByTenantIdAndOccurrenceDateBetween(
            Long tenantId, LocalDate startDate, LocalDate endDate);

    boolean existsBySubscriptionIdAndOccurrenceDate(Long subscriptionId, LocalDate occurrenceDate);
}

