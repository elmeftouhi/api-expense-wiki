package com.elmeftouhi.expense.multitenancy;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@MappedSuperclass
@EntityListeners(TenantEntityListener.class)
@Getter
public abstract class BaseTenantAwareEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    public void assignTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}

