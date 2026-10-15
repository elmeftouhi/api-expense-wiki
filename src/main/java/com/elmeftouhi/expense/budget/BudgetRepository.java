package com.elmeftouhi.expense.budget;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByIdAndTenantId(Long id, Long tenantId);

    List<Budget> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    @Query("""
            SELECT DISTINCT b
            FROM Budget b
            JOIN b.categories c
            WHERE b.tenantId = :tenantId
                AND b.active = true
                AND c.id = :categoryId
            """)
    List<Budget> findActiveByTenantIdAndCategoryId(
            @Param("tenantId") Long tenantId,
            @Param("categoryId") Long categoryId);
}

