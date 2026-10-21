package com.elmeftouhi.expense.expense.repository;

import com.elmeftouhi.expense.expense.entity.SubscriptionPayment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {

    Optional<SubscriptionPayment> findByIdAndTenantId(Long id, Long tenantId);

    List<SubscriptionPayment> findBySubscriptionId(Long subscriptionId);

    Page<SubscriptionPayment> findBySubscriptionIdAndTenantIdOrderByScheduledBillingDateDesc(
            Long subscriptionId,
            Long tenantId,
            Pageable pageable);

    List<SubscriptionPayment> findByTenantIdAndScheduledBillingDateBetween(
            Long tenantId, LocalDate startDate, LocalDate endDate);

    boolean existsByTenantIdAndSubscriptionIdAndScheduledBillingDate(
            Long tenantId,
            Long subscriptionId,
            LocalDate scheduledBillingDate);

    Optional<SubscriptionPayment> findByTenantIdAndSubscriptionIdAndScheduledBillingDate(
            Long tenantId,
            Long subscriptionId,
            LocalDate scheduledBillingDate);
}



