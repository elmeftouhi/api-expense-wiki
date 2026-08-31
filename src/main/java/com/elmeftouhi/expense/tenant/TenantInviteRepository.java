package com.elmeftouhi.expense.tenant;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantInviteRepository extends JpaRepository<TenantInvite, Long> {

    Optional<TenantInvite> findByCodeIgnoreCase(String code);

    boolean existsByCode(String code);

    @Modifying
    @Query("""
            update TenantInvite ti
               set ti.usedAt = :usedAt
             where ti.id = :inviteId
               and ti.usedAt is null
               and ti.expiresAt > :now
            """)
    int consumeInviteIfActive(@Param("inviteId") Long inviteId, @Param("usedAt") Instant usedAt, @Param("now") Instant now);
}

