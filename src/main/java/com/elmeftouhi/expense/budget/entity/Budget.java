package com.elmeftouhi.expense.budget.entity;

import com.elmeftouhi.expense.budget.BudgetPeriodicity;
import com.elmeftouhi.expense.budget.BudgetWarningType;
import com.elmeftouhi.expense.category.entity.Category;
import com.elmeftouhi.expense.multitenancy.BaseTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "budgets",
        indexes = {
                @Index(name = "idx_budgets_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_budgets_tenant_periodicity", columnList = "tenant_id, periodicity"),
                @Index(name = "idx_budgets_tenant_active", columnList = "tenant_id, is_active")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Budget extends BaseTenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "periodicity", nullable = false, length = 20)
    private BudgetPeriodicity periodicity;

    @Column(name = "allocated_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal allocatedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "warning_type", nullable = false, length = 20)
    private BudgetWarningType warningType;

    @Column(name = "warning_threshold", precision = 19, scale = 2, nullable = false)
    private BigDecimal warningThreshold;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "budget_categories",
            joinColumns = @JoinColumn(name = "budget_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @OrderBy("name ASC")
    private Set<Category> categories = new HashSet<>();

    @OneToMany(mappedBy = "budget", fetch = FetchType.LAZY)
    private Set<BudgetPeriodInstance> periodInstances = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();
}

