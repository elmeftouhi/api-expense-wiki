package com.elmeftouhi.expense.tenantconfig.repository;

import com.elmeftouhi.expense.tenantconfig.entity.TenantConfiguration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantConfigurationRepository extends JpaRepository<TenantConfiguration, Long> {
    Optional<TenantConfiguration> findByTenantId(Long tenantId);
}

