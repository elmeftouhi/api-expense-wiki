package com.elmeftouhi.expense.tenantconfig.dto;

import com.elmeftouhi.expense.tenantconfig.Currency;
import com.elmeftouhi.expense.tenantconfig.Language;
import com.elmeftouhi.expense.tenantconfig.TimeFormat;
import com.elmeftouhi.expense.tenantconfig.WeekStartDay;
import java.time.Instant;

public record TenantConfigurationResponse(
        Long id,
        Long tenantId,
        Currency currency,
        Integer decimalPlaces,
        Language language,
        String timezone,
        String dateFormat,
        TimeFormat timeFormat,
        WeekStartDay weekStartDay,
        Integer budgetAlertThreshold,
        String notificationChannels,
        String autoPaymentReminderDays,
        String expenseRoundingMethod,
        String reportingPeriod,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy
) {
}

