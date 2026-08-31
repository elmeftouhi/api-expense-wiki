package com.elmeftouhi.expense.multitenancy;

import com.elmeftouhi.expense.shared.exception.TenantAccessDeniedException;
import jakarta.persistence.PrePersist;
import java.util.Objects;

public class TenantEntityListener {

    @PrePersist
    public void prePersist(Object target) {
        if (!(target instanceof BaseTenantAwareEntity tenantAwareEntity)) {
            return;
        }

        Long currentTenantId = TenantContext.getRequiredTenantId();
        Long entityTenantId = tenantAwareEntity.getTenantId();

        if (entityTenantId == null) {
            tenantAwareEntity.assignTenantId(currentTenantId);
            return;
        }

        if (!Objects.equals(entityTenantId, currentTenantId)) {
            throw new TenantAccessDeniedException("Cross-tenant write is not allowed");
        }
    }
}

