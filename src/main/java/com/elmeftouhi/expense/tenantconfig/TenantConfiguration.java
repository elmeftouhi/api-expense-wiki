package com.elmeftouhi.expense.tenantconfig;

import com.elmeftouhi.expense.multitenancy.BaseTenantAwareEntity;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "tenant_configurations",
        indexes = {
                @Index(name = "idx_tenant_config_tenant_id", columnList = "tenant_id", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TenantConfiguration extends BaseTenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private Currency currency = Currency.USD;

    @Column(name = "decimal_places", nullable = false)
    private Integer decimalPlaces = 2;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 10)
    private Language language = Language.EN;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone = "UTC";

    @Column(name = "date_format", nullable = false, length = 20)
    private String dateFormat = "yyyy-MM-dd";

    @Enumerated(EnumType.STRING)
    @Column(name = "time_format", nullable = false, length = 10)
    private TimeFormat timeFormat = TimeFormat.FORMAT_24H;

    @Enumerated(EnumType.STRING)
    @Column(name = "week_start_day", nullable = false, length = 10)
    private WeekStartDay weekStartDay = WeekStartDay.MONDAY;

    @Column(name = "budget_alert_threshold", nullable = false)
    private Integer budgetAlertThreshold = 80;

    @Column(name = "notification_channels", length = 200)
    private String notificationChannels = "[\"IN_APP\"]";

    @Column(name = "auto_payment_reminder_days", length = 100)
    private String autoPaymentReminderDays = "[3,7]";

    @Column(name = "expense_rounding_method", nullable = false, length = 20)
    private String expenseRoundingMethod = "NONE";

    @Column(name = "reporting_period", nullable = false, length = 20)
    private String reportingPeriod = "CALENDAR_MONTH";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "updated_by", length = 120)
    private String updatedBy;
}

