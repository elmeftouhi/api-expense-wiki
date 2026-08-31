package com.elmeftouhi.expense.category;

import com.elmeftouhi.expense.multitenancy.BaseTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "categories",
        indexes = {
                @Index(name = "idx_categories_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_categories_tenant_name", columnList = "tenant_id, name")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Category extends BaseTenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 100)
    private String icon;

    @Column(name = "text_color", length = 20)
    private String textColor;

    @Column(name = "background_color", length = 20)
    private String backgroundColor;

    @Column(name = "monthly_budget", precision = 19, scale = 2)
    private BigDecimal monthlyBudget;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "allow_over_budget", nullable = false)
    private boolean allowOverBudget = false;
}

