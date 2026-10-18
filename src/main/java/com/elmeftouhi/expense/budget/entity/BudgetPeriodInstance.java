package com.elmeftouhi.expense.budget.entity;

import com.elmeftouhi.expense.budget.BudgetPeriodStatus;
import com.elmeftouhi.expense.budget.BudgetWarningType;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "budget_period_instances",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_budget_period_instances_budget_start", columnNames = {"budget_id", "start_date"})
        },
        indexes = {
                @Index(name = "idx_budget_period_instances_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_budget_period_instances_budget_id", columnList = "budget_id"),
                @Index(name = "idx_budget_period_instances_tenant_dates", columnList = "tenant_id, start_date, end_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class BudgetPeriodInstance extends BaseTenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "allocated_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal allocatedAmount;

    @Column(name = "spent_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal spentAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BudgetPeriodStatus status = BudgetPeriodStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "warning_type", nullable = false, length = 20)
    private BudgetWarningType warningType;

    @Column(name = "warning_threshold", precision = 19, scale = 2, nullable = false)
    private BigDecimal warningThreshold;

    @Column(name = "warning_sent", nullable = false)
    private boolean warningSent = false;

    @Column(name = "exceeded_sent", nullable = false)
    private boolean exceededSent = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();
}

