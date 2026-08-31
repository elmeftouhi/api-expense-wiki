package com.elmeftouhi.expense.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface UserTenantRepository extends JpaRepository<UserTenant, Long> {

    boolean existsByUserIdAndTenantId(Long userId, Long tenantId);

    @EntityGraph(attributePaths = {"tenant", "user", "user.defaultTenant"})
    List<UserTenant> findAllByUserId(Long userId);

    @EntityGraph(attributePaths = {"tenant", "user", "user.defaultTenant"})
    Optional<UserTenant> findByUserIdAndTenantId(Long userId, Long tenantId);

    long deleteByUserIdAndTenantId(Long userId, Long tenantId);

    long deleteAllByUserId(Long userId);
}

