package com.elmeftouhi.expense.expense.repository;

import com.elmeftouhi.expense.expense.entity.Expense;
import com.elmeftouhi.expense.expense.ExpenseStatus;
import com.elmeftouhi.expense.expense.ExpenseType;
import java.time.LocalDate;
import java.util.Collection;
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

    List<Expense> findByTenantIdAndDeletedFalseOrderByDueDateDesc(Long tenantId);

    List<Expense> findByTenantIdAndCategoryIdAndDeletedFalse(Long tenantId, Long categoryId);

    List<Expense> findByTenantIdAndStatusAndDeletedFalse(Long tenantId, ExpenseStatus status);

    List<Expense> findByTenantIdAndStatusAndDueDateBeforeAndDeletedFalse(
            Long tenantId, ExpenseStatus status, LocalDate date);

    List<Expense> findByTenantIdAndDueDateBetweenAndDeletedFalse(
            Long tenantId, LocalDate start, LocalDate end);

    List<Expense> findByTenantIdAndStatusAndDueDateBetweenAndDeletedFalse(
            Long tenantId,
            ExpenseStatus status,
            LocalDate start,
            LocalDate end);

    @Query("""
            SELECT e FROM Expense e
            WHERE e.tenantId = :tenantId
                AND e.deleted = false
                AND e.dueDate BETWEEN :startDate AND :endDate
                AND (:categoryIds IS NULL OR e.category.id IN :categoryIds)
                AND (:type IS NULL OR e.type = :type)
                AND (:status IS NULL OR e.status = :status)
                AND (:applySearchFilter = false
                    OR LOWER(COALESCE(e.description, '')) LIKE :searchPattern
                    OR CAST(e.amount AS string) LIKE :searchPattern)
            """)
    Page<Expense> findPageByTenantAndFilters(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryIds") Collection<Long> categoryIds,
            @Param("type") ExpenseType type,
            @Param("status") ExpenseStatus status,
            @Param("applySearchFilter") boolean applySearchFilter,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM Expense e
            WHERE e.tenantId = :tenantId
                AND e.deleted = false
                AND e.dueDate BETWEEN :startDate AND :endDate
                AND (:categoryIds IS NULL OR e.category.id IN :categoryIds)
                AND (:type IS NULL OR e.type = :type)
                AND (:status IS NULL OR e.status = :status)
                AND (:applySearchFilter = false
                    OR LOWER(COALESCE(e.description, '')) LIKE :searchPattern
                    OR CAST(e.amount AS string) LIKE :searchPattern)
            """)
    java.math.BigDecimal sumByTenantAndFilters(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryIds") Collection<Long> categoryIds,
            @Param("type") ExpenseType type,
            @Param("status") ExpenseStatus status,
            @Param("applySearchFilter") boolean applySearchFilter,
            @Param("searchPattern") String searchPattern);

    @Query("""
            SELECT e FROM Expense e
            WHERE e.tenantId = :tenantId
                AND e.deleted = false
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
                AND e.dueDate BETWEEN :startDate AND :endDate
            """)
    java.math.BigDecimal sumByCategoryAndDateRange(
            @Param("tenantId") Long tenantId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM Expense e
            WHERE e.tenantId = :tenantId
                AND e.deleted = false
                AND e.status = :status
                AND e.category.id IN :categoryIds
                AND e.dueDate BETWEEN :startDate AND :endDate
            """)
    java.math.BigDecimal sumByTenantStatusCategoriesAndDueDateRange(
            @Param("tenantId") Long tenantId,
            @Param("status") ExpenseStatus status,
            @Param("categoryIds") java.util.Collection<Long> categoryIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM Expense e
            WHERE e.tenantId = :tenantId
                AND e.deleted = false
                AND e.status IN :statuses
                AND e.dueDate BETWEEN :startDate AND :endDate
            """)
    java.math.BigDecimal sumByTenantAndStatusesAndDueDateRange(
            @Param("tenantId") Long tenantId,
            @Param("statuses") Collection<ExpenseStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM Expense e
            WHERE e.tenantId = :tenantId
                AND e.deleted = false
                AND e.status IN :statuses
                AND e.dueDate < :date
            """)
    java.math.BigDecimal sumByTenantAndStatusesAndDueDateBefore(
            @Param("tenantId") Long tenantId,
            @Param("statuses") Collection<ExpenseStatus> statuses,
            @Param("date") LocalDate date);

    @Query("""
            SELECT COUNT(e)
            FROM Expense e
            WHERE e.tenantId = :tenantId
                AND e.deleted = false
                AND e.status IN :statuses
                AND e.dueDate < :date
            """)
    long countByTenantAndStatusesAndDueDateBefore(
            @Param("tenantId") Long tenantId,
            @Param("statuses") Collection<ExpenseStatus> statuses,
            @Param("date") LocalDate date);

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM Expense e
            WHERE e.tenantId = :tenantId
                AND e.deleted = false
                AND e.status = :status
                AND e.dueDate >= :date
            """)
    java.math.BigDecimal sumByTenantAndStatusAndDueDateOnOrAfter(
            @Param("tenantId") Long tenantId,
            @Param("status") ExpenseStatus status,
            @Param("date") LocalDate date);

    @Query("""
            SELECT COUNT(e)
            FROM Expense e
            WHERE e.tenantId = :tenantId
                AND e.deleted = false
                AND e.status = :status
                AND e.dueDate >= :date
            """)
    long countByTenantAndStatusAndDueDateOnOrAfter(
            @Param("tenantId") Long tenantId,
            @Param("status") ExpenseStatus status,
            @Param("date") LocalDate date);

    long countByTenantIdAndStatusAndDueDateBetweenAndDeletedFalse(
            Long tenantId,
            ExpenseStatus status,
            LocalDate startDate,
            LocalDate endDate);

    Optional<Expense> findFirstByTenantIdAndStatusAndDueDateBetweenAndDeletedFalseOrderByDueDateAsc(
            Long tenantId,
            ExpenseStatus status,
            LocalDate startDate,
            LocalDate endDate);

    boolean existsByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);
}

