package com.elmeftouhi.expense.budget.repository;

import com.elmeftouhi.expense.budget.entity.BudgetPeriodInstance;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetPeriodInstanceRepository extends JpaRepository<BudgetPeriodInstance, Long> {

    interface CurrentBudgetTotalsView {
        java.math.BigDecimal getAllocatedAmount();

        java.math.BigDecimal getSpentAmount();
    }

    Optional<BudgetPeriodInstance> findByIdAndTenantId(Long id, Long tenantId);

    Optional<BudgetPeriodInstance> findByBudgetIdAndTenantIdAndStartDate(Long budgetId, Long tenantId, LocalDate startDate);

    @Query("""
            SELECT p
            FROM BudgetPeriodInstance p
            WHERE p.budget.id = :budgetId
                AND p.tenantId = :tenantId
                AND :date BETWEEN p.startDate AND p.endDate
            """)
    Optional<BudgetPeriodInstance> findByBudgetIdAndTenantIdAndDate(
            @Param("budgetId") Long budgetId,
            @Param("tenantId") Long tenantId,
            @Param("date") LocalDate date);

    Optional<BudgetPeriodInstance> findTopByBudgetIdAndTenantIdAndStartDateBeforeOrderByStartDateDesc(
            Long budgetId,
            Long tenantId,
            LocalDate startDate);

    List<BudgetPeriodInstance> findByBudgetIdAndTenantIdOrderByStartDateDesc(Long budgetId, Long tenantId);

    @Query("""
            SELECT p
            FROM BudgetPeriodInstance p
            WHERE p.budget.id = :budgetId
                AND p.tenantId = :tenantId
                AND :date BETWEEN p.startDate AND p.endDate
            """)
    Optional<BudgetPeriodInstance> findCurrentByBudgetId(
            @Param("budgetId") Long budgetId,
            @Param("tenantId") Long tenantId,
            @Param("date") LocalDate date);

    @Query("""
            SELECT
                COALESCE(SUM(p.allocatedAmount), 0) AS allocatedAmount,
                COALESCE(SUM(p.spentAmount), 0) AS spentAmount
            FROM BudgetPeriodInstance p
            WHERE p.tenantId = :tenantId
                AND p.budget.active = true
                AND :date BETWEEN p.startDate AND p.endDate
            """)
    CurrentBudgetTotalsView getCurrentTotals(
            @Param("tenantId") Long tenantId,
            @Param("date") LocalDate date);
}

