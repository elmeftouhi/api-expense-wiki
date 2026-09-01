package com.elmeftouhi.expense.tenantconfig;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantConfigurationRepository extends JpaRepository<TenantConfiguration, Long> {
    Optional<TenantConfiguration> findByTenantId(Long tenantId);
}

