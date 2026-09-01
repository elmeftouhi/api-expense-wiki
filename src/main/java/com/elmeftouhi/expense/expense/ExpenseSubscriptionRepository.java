package com.elmeftouhi.expense.expense;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseSubscriptionRepository extends JpaRepository<ExpenseSubscription, Long> {

    Optional<ExpenseSubscription> findByExpenseId(Long expenseId);

    Optional<ExpenseSubscription> findByIdAndTenantId(Long id, Long tenantId);

    List<ExpenseSubscription> findByTenantIdAndActiveTrue(Long tenantId);

    List<ExpenseSubscription> findByTenantIdAndActiveTrueAndNextOccurrenceDateLessThanEqual(
            Long tenantId, LocalDate date);

    @Query("""
            SELECT s FROM ExpenseSubscription s
            WHERE s.tenantId = :tenantId
                AND s.active = true
                AND s.nextOccurrenceDate <= :targetDate
                AND (s.endDate IS NULL OR s.endDate >= :targetDate)
            ORDER BY s.nextOccurrenceDate ASC
            """)
    List<ExpenseSubscription> findDueSubscriptions(
            @Param("tenantId") Long tenantId,
            @Param("targetDate") LocalDate targetDate);

    boolean existsByIdAndTenantId(Long id, Long tenantId);
}

