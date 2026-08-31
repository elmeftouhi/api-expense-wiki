package com.elmeftouhi.expense.category;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByTenantIdOrderByNameAsc(Long tenantId);

    Optional<Category> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Category> findByTenantIdAndNameIgnoreCase(Long tenantId, String name);
}

