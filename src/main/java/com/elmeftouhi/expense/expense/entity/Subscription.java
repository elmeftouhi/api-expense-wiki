package com.elmeftouhi.expense.expense.entity;

import com.elmeftouhi.expense.expense.SubscriptionFrequency;
import com.elmeftouhi.expense.expense.SubscriptionLifecycleStatus;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "subscriptions",
        indexes = {
                @Index(name = "idx_subscriptions_v2_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_subscriptions_v2_category_id", columnList = "category_id"),
                @Index(name = "idx_subscriptions_v2_status", columnList = "status"),
                @Index(name = "idx_subscriptions_v2_next_billing_date", columnList = "next_billing_date"),
                @Index(name = "idx_subscriptions_v2_tenant_status_next_date", columnList = "tenant_id, status, next_billing_date"),
                @Index(name = "idx_subscriptions_v2_legacy_expense_id", columnList = "legacy_template_expense_id"),
                @Index(name = "idx_subscriptions_v2_legacy_subscription_id", columnList = "legacy_subscription_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Subscription extends BaseTenantAwareEntity {

    public static final int DEFAULT_REMINDER_LEAD_DAYS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 10, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private SubscriptionFrequency frequency;

    @Column(name = "custom_interval_days")
    private Integer customIntervalDays;

    @Column(name = "next_billing_date", nullable = false)
    private LocalDate nextBillingDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionLifecycleStatus status = SubscriptionLifecycleStatus.ACTIVE;

    @Column(name = "reminder_lead_days", nullable = false)
    private Integer reminderLeadDays = DEFAULT_REMINDER_LEAD_DAYS;

    @Column(name = "payment_method", length = 100)
    private String paymentMethod;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "anchor_day_of_month", nullable = false)
    private Integer anchorDayOfMonth;

    @Column(name = "anchor_month_of_year", nullable = false)
    private Integer anchorMonthOfYear;

    @Column(name = "legacy_template_expense_id")
    private Long legacyTemplateExpenseId;

    @Column(name = "legacy_subscription_id")
    private Long legacySubscriptionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public boolean isActiveFor(LocalDate businessDate) {
        if (status != SubscriptionLifecycleStatus.ACTIVE) {
            return false;
        }
        if (businessDate.isBefore(nextBillingDate)) {
            return false;
        }
        return endDate == null || !nextBillingDate.isAfter(endDate);
    }

    public LocalDate calculateNextBillingDate() {
        return frequency.advance(nextBillingDate, customIntervalDays, anchorDayOfMonth, anchorMonthOfYear);
    }

    public void refreshAnchorsFromNextBillingDate() {
        this.anchorDayOfMonth = nextBillingDate.getDayOfMonth();
        this.anchorMonthOfYear = nextBillingDate.getMonthValue();
    }
}

