package com.elmeftouhi.expense.tenantconfig;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class TenantConfigurationSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initializeTenantConfigurations() {
        // Ensure default configurations exist for all tenants
        String sql = """
                INSERT INTO tenant_configurations (
                    tenant_id, currency, decimal_places, language, timezone,
                    date_format, time_format, week_start_day, budget_alert_threshold,
                    notification_channels, auto_payment_reminder_days,
                    expense_rounding_method, reporting_period, created_at, updated_at
                )
                SELECT
                    t.id, 'USD', 2, 'EN', 'UTC',
                    'yyyy-MM-dd', '24H', 'MONDAY', 80,
                    '[\"IN_APP\"]', '[3,7]',
                    'NONE', 'CALENDAR_MONTH', NOW(), NOW()
                FROM tenants t
                WHERE NOT EXISTS (
                    SELECT 1 FROM tenant_configurations tc WHERE tc.tenant_id = t.id
                )
                """;
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            // Table might not exist yet, skip
        }
    }
}

