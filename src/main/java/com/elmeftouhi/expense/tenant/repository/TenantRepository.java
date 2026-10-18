package com.elmeftouhi.expense.tenant.repository;

import com.elmeftouhi.expense.tenant.entity.Tenant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByName(String name);

    boolean existsByNameIgnoreCase(String name);
}

