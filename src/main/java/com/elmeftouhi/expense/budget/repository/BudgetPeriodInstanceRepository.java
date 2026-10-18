package com.elmeftouhi.expense.budget.repository;

import com.elmeftouhi.expense.budget.entity.BudgetPeriodInstance;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetPeriodInstanceRepository extends JpaRepository<BudgetPeriodInstance, Long> {

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
}

