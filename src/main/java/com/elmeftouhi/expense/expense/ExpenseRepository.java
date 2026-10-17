package com.elmeftouhi.expense.expense;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Optional<Expense> findByIdAndTenantId(Long id, Long tenantId);

    List<Expense> findByTenantIdAndDeletedFalseAndSubscriptionTemplateFalseOrderByDueDateDesc(Long tenantId);

    List<Expense> findByTenantIdAndCategoryIdAndDeletedFalseAndSubscriptionTemplateFalse(Long tenantId, Long categoryId);

    List<Expense> findByTenantIdAndStatusAndDeletedFalseAndSubscriptionTemplateFalse(Long tenantId, ExpenseStatus status);

    List<Expense> findByTenantIdAndStatusAndDueDateBeforeAndDeletedFalseAndSubscriptionTemplateFalse(
            Long tenantId, ExpenseStatus status, LocalDate date);

    List<Expense> findByTenantIdAndDueDateBetweenAndDeletedFalseAndSubscriptionTemplateFalse(
            Long tenantId, LocalDate start, LocalDate end);

    List<Expense> findByTenantIdAndStatusAndDueDateBetweenAndDeletedFalseAndSubscriptionTemplateFalse(
            Long tenantId,
            ExpenseStatus status,
            LocalDate start,
            LocalDate end);

    @Query("""
            SELECT e FROM Expense e
            WHERE e.tenantId = :tenantId
                AND e.deleted = false
                AND e.subscriptionTemplate = false
                AND e.dueDate BETWEEN :startDate AND :endDate
                AND (:categoryId IS NULL OR e.category.id = :categoryId)
                AND (:type IS NULL OR e.type = :type)
            """)
    Page<Expense> findPageByTenantAndFilters(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryId") Long categoryId,
            @Param("type") ExpenseType type,
            Pageable pageable);

    @Query("""
            SELECT e FROM Expense e
            WHERE e.tenantId = :tenantId
                AND e.deleted = false
                AND e.subscriptionTemplate = false
                AND e.status = :status
                AND e.dueDate >= :startDate
                AND e.dueDate <= :endDate
                AND (:categoryId IS NULL OR e.category.id = :categoryId)
            ORDER BY e.dueDate DESC
            """)
    List<Expense> findByTenantWithFilters(
            @Param("tenantId") Long tenantId,
            @Param("status") ExpenseStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryId") Long categoryId);

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM Expense e
            WHERE e.tenantId = :tenantId
                AND e.deleted = false
                AND e.subscriptionTemplate = false
                AND e.status = :status
                AND e.dueDate BETWEEN :startDate AND :endDate
            """)
    java.math.BigDecimal sumByTenantAndStatusAndDateRange(
            @Param("tenantId") Long tenantId,
            @Param("status") ExpenseStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM Expense e
            WHERE e.tenantId = :tenantId
                AND e.category.id = :categoryId
                AND e.deleted = false
                AND e.subscriptionTemplate = false
                AND e.dueDate BETWEEN :startDate AND :endDate
            """)
    java.math.BigDecimal sumByCategoryAndDateRange(
            @Param("tenantId") Long tenantId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsByIdAndTenantIdAndDeletedFalseAndSubscriptionTemplateFalse(Long id, Long tenantId);
}

