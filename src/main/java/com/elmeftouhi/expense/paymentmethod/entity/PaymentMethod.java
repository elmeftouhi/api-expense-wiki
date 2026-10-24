package com.elmeftouhi.expense.paymentmethod.entity;

import com.elmeftouhi.expense.multitenancy.BaseTenantAwareEntity;
import com.elmeftouhi.expense.paymentmethod.PaymentMethodType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "payment_methods",
        indexes = {
                @Index(name = "idx_payment_methods_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_payment_methods_tenant_default", columnList = "tenant_id, is_default")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PaymentMethod extends BaseTenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private PaymentMethodType type;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();
}

