package com.elmeftouhi.expense.expense;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "expense_subscriptions",
        indexes = {
                @Index(name = "idx_subscriptions_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_subscriptions_expense_id", columnList = "expense_id"),
                @Index(name = "idx_subscriptions_next_date", columnList = "next_occurrence_date"),
                @Index(name = "idx_subscriptions_active", columnList = "is_active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSubscription extends BaseTenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false, unique = true)
    private Expense expense;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private SubscriptionFrequency frequency;

    @Column(name = "custom_interval_days")
    private Integer customIntervalDays;

    @Column(name = "next_occurrence_date", nullable = false)
    private LocalDate nextOccurrenceDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "occurrence_count", nullable = false)
    private Integer occurrenceCount = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public boolean shouldGenerateNextOccurrence(LocalDate today) {
        if (!active) {
            return false;
        }
        if (endDate != null && today.isAfter(endDate)) {
            return false;
        }
        return !today.isBefore(nextOccurrenceDate);
    }

    public LocalDate calculateNextOccurrenceDate() {
        if (frequency == SubscriptionFrequency.CUSTOM && customIntervalDays != null) {
            return nextOccurrenceDate.plusDays(customIntervalDays);
        }
        return nextOccurrenceDate.plusDays(frequency.getDays());
    }
}

