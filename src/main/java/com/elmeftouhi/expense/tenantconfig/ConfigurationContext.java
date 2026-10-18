package com.elmeftouhi.expense.tenantconfig;

import com.elmeftouhi.expense.tenantconfig.entity.TenantConfiguration;
import com.elmeftouhi.expense.shared.exception.BadRequestException;

/**
 * Thread-local context for tenant configuration.
 * Loaded per request in TenantContextFilter.
 */
public final class ConfigurationContext {

    private static final ThreadLocal<TenantConfiguration> CURRENT_CONFIG = new ThreadLocal<>();

    private ConfigurationContext() {
    }

    public static void setConfiguration(TenantConfiguration config) {
        CURRENT_CONFIG.set(config);
    }

    public static TenantConfiguration getConfiguration() {
        TenantConfiguration config = CURRENT_CONFIG.get();
        if (config == null) {
            throw new BadRequestException("No configuration loaded for this request");
        }
        return config;
    }

    public static Currency getCurrency() {
        return getConfiguration().getCurrency();
    }

    public static Integer getDecimalPlaces() {
        return getConfiguration().getDecimalPlaces();
    }

    public static Language getLanguage() {
        return getConfiguration().getLanguage();
    }

    public static String getTimezone() {
        return getConfiguration().getTimezone();
    }

    public static String getDateFormat() {
        return getConfiguration().getDateFormat();
    }

    public static TimeFormat getTimeFormat() {
        return getConfiguration().getTimeFormat();
    }

    public static WeekStartDay getWeekStartDay() {
        return getConfiguration().getWeekStartDay();
    }

    public static Integer getBudgetAlertThreshold() {
        return getConfiguration().getBudgetAlertThreshold();
    }

    public static String getNotificationChannels() {
        return getConfiguration().getNotificationChannels();
    }

    public static String getAutoPaymentReminderDays() {
        return getConfiguration().getAutoPaymentReminderDays();
    }

    public static String getExpenseRoundingMethod() {
        return getConfiguration().getExpenseRoundingMethod();
    }

    public static String getReportingPeriod() {
        return getConfiguration().getReportingPeriod();
    }

    public static void clear() {
        CURRENT_CONFIG.remove();
    }
}

