package com.elmeftouhi.expense.expense.repository;

import com.elmeftouhi.expense.expense.SubscriptionFrequency;
import com.elmeftouhi.expense.expense.entity.Subscription;
import com.elmeftouhi.expense.expense.SubscriptionLifecycleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Subscription> findByLegacyTemplateExpenseIdAndTenantId(Long legacyTemplateExpenseId, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subscription s WHERE s.id = :id AND s.tenantId = :tenantId")
    Optional<Subscription> findForUpdate(
            @Param("id") Long id,
            @Param("tenantId") Long tenantId);

    @Query("""
            SELECT s FROM Subscription s
            WHERE s.tenantId = :tenantId
                AND (:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(s.description, '')) LIKE LOWER(CONCAT('%', :search, '%')))
                AND (:status IS NULL OR s.status = :status)
                AND (:categoryId IS NULL OR s.category.id = :categoryId)
                AND (:frequency IS NULL OR s.frequency = :frequency)
                AND (:nextBillingFrom IS NULL OR s.nextBillingDate >= :nextBillingFrom)
                AND (:nextBillingTo IS NULL OR s.nextBillingDate <= :nextBillingTo)
            """)
    Page<Subscription> search(
            @Param("tenantId") Long tenantId,
            @Param("search") String search,
            @Param("status") SubscriptionLifecycleStatus status,
            @Param("categoryId") Long categoryId,
            @Param("frequency") SubscriptionFrequency frequency,
            @Param("nextBillingFrom") LocalDate nextBillingFrom,
            @Param("nextBillingTo") LocalDate nextBillingTo,
            Pageable pageable);

    @Query("""
            SELECT s FROM Subscription s
            WHERE s.tenantId = :tenantId
                AND (:status IS NULL OR s.status = :status)
                AND (:categoryId IS NULL OR s.category.id = :categoryId)
                AND (:frequency IS NULL OR s.frequency = :frequency)
                AND (:nextBillingFrom IS NULL OR s.nextBillingDate >= :nextBillingFrom)
                AND (:nextBillingTo IS NULL OR s.nextBillingDate <= :nextBillingTo)
            """)
    Page<Subscription> searchWithoutText(
            @Param("tenantId") Long tenantId,
            @Param("status") SubscriptionLifecycleStatus status,
            @Param("categoryId") Long categoryId,
            @Param("frequency") SubscriptionFrequency frequency,
            @Param("nextBillingFrom") LocalDate nextBillingFrom,
            @Param("nextBillingTo") LocalDate nextBillingTo,
            Pageable pageable);

    @Query("""
            SELECT s FROM Subscription s
            WHERE s.tenantId = :tenantId
                AND s.status = com.elmeftouhi.expense.expense.SubscriptionLifecycleStatus.ACTIVE
                AND s.nextBillingDate <= :businessDate
                AND (s.endDate IS NULL OR s.nextBillingDate <= s.endDate)
            ORDER BY s.nextBillingDate ASC
            """)
    List<Subscription> findDueForGeneration(
            @Param("tenantId") Long tenantId,
            @Param("businessDate") LocalDate businessDate);

    @Query("""
            SELECT s FROM Subscription s
            WHERE s.tenantId = :tenantId
                AND s.status = com.elmeftouhi.expense.expense.SubscriptionLifecycleStatus.ACTIVE
                AND s.nextBillingDate > :businessDate
                AND s.nextBillingDate <= :upperDate
            ORDER BY s.nextBillingDate ASC
            """)
    List<Subscription> findUpcomingForReminderWindow(
            @Param("tenantId") Long tenantId,
            @Param("businessDate") LocalDate businessDate,
            @Param("upperDate") LocalDate upperDate);
}


