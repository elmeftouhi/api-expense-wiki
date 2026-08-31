package com.elmeftouhi.expense.category;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.tenantId = :tenantId AND c.parentCategory IS NULL AND (:includeInactive = true OR c.active = true) ORDER BY COALESCE(c.level, 0) ASC, c.name ASC")
    List<Category> findRootCategories(
            @Param("tenantId") Long tenantId,
            @Param("includeInactive") boolean includeInactive
    );

    @Query("SELECT c FROM Category c WHERE c.tenantId = :tenantId AND c.parentCategory.id = :parentCategoryId AND (:includeInactive = true OR c.active = true) ORDER BY COALESCE(c.level, 0) ASC, c.name ASC")
    List<Category> findChildCategories(
            @Param("tenantId") Long tenantId,
            @Param("parentCategoryId") Long parentCategoryId,
            @Param("includeInactive") boolean includeInactive
    );

    Optional<Category> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Category> findByTenantIdAndNameIgnoreCase(Long tenantId, String name);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c WHERE c.tenantId = :tenantId AND c.parentCategory.id = :parentCategoryId")
    boolean existsByTenantIdAndParentCategoryId(
            @Param("tenantId") Long tenantId,
            @Param("parentCategoryId") Long parentCategoryId
    );
}

