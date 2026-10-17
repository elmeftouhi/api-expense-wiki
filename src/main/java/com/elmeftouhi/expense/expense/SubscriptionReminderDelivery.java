package com.elmeftouhi.expense.expense;

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
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "subscription_reminder_deliveries",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_subscription_reminder_delivery",
                        columnNames = {"tenant_id", "subscription_id", "scheduled_billing_date", "user_id", "channel"}
                )
        },
        indexes = {
                @Index(name = "idx_subscription_reminder_tenant", columnList = "tenant_id"),
                @Index(name = "idx_subscription_reminder_subscription", columnList = "subscription_id"),
                @Index(name = "idx_subscription_reminder_date", columnList = "scheduled_billing_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionReminderDelivery extends BaseTenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "scheduled_billing_date", nullable = false)
    private LocalDate scheduledBillingDate;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "channel", nullable = false, length = 30)
    private String channel;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt = Instant.now();
}

