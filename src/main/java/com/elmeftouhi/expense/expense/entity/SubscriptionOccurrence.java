package com.elmeftouhi.expense.expense.entity;

import com.elmeftouhi.expense.multitenancy.BaseTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "subscription_billing_occurrences",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_subscription_billing_occurrence_tenant_subscription_date",
                        columnNames = {"tenant_id", "subscription_id", "scheduled_billing_date"}
                )
        },
        indexes = {
                @Index(name = "idx_subs_billing_occ_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_subs_billing_occ_subscription_id", columnList = "subscription_id"),
                @Index(name = "idx_subs_billing_occ_generated_expense", columnList = "generated_expense_id"),
                @Index(name = "idx_subs_billing_occ_scheduled_date", columnList = "scheduled_billing_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionOccurrence extends BaseTenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_expense_id", nullable = false)
    private Expense generatedExpense;

    @Column(name = "scheduled_billing_date", nullable = false)
    private LocalDate scheduledBillingDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

