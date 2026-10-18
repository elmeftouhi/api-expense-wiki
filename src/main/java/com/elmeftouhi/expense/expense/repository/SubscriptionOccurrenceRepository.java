package com.elmeftouhi.expense.expense.repository;

import com.elmeftouhi.expense.expense.entity.SubscriptionOccurrence;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionOccurrenceRepository extends JpaRepository<SubscriptionOccurrence, Long> {

    List<SubscriptionOccurrence> findBySubscriptionId(Long subscriptionId);

    Page<SubscriptionOccurrence> findBySubscriptionIdAndTenantIdOrderByScheduledBillingDateDesc(
            Long subscriptionId,
            Long tenantId,
            Pageable pageable);

    List<SubscriptionOccurrence> findByTenantIdAndScheduledBillingDateBetween(
            Long tenantId, LocalDate startDate, LocalDate endDate);

    boolean existsByTenantIdAndSubscriptionIdAndScheduledBillingDate(
            Long tenantId,
            Long subscriptionId,
            LocalDate scheduledBillingDate);

    Optional<SubscriptionOccurrence> findByTenantIdAndSubscriptionIdAndScheduledBillingDate(
            Long tenantId,
            Long subscriptionId,
            LocalDate scheduledBillingDate);
}

