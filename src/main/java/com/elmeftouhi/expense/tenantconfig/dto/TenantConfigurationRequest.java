package com.elmeftouhi.expense.tenantconfig.dto;

import com.elmeftouhi.expense.tenantconfig.Currency;
import com.elmeftouhi.expense.tenantconfig.Language;
import com.elmeftouhi.expense.tenantconfig.TimeFormat;
import com.elmeftouhi.expense.tenantconfig.WeekStartDay;

public record TenantConfigurationRequest(
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
        String reportingPeriod
) {
}

