package com.elmeftouhi.expense.datatransfer.dto;

import com.elmeftouhi.expense.activity.ActivityAction;
import com.elmeftouhi.expense.activity.ActivitySubjectType;
import com.elmeftouhi.expense.budget.BudgetPeriodStatus;
import com.elmeftouhi.expense.budget.BudgetPeriodicity;
import com.elmeftouhi.expense.budget.BudgetWarningType;
import com.elmeftouhi.expense.expense.ExpenseStatus;
import com.elmeftouhi.expense.expense.ExpenseType;
import com.elmeftouhi.expense.expense.SubscriptionFrequency;
import com.elmeftouhi.expense.expense.SubscriptionLifecycleStatus;
import com.elmeftouhi.expense.exercice.ExerciceStatus;
import com.elmeftouhi.expense.notification.NotificationStatus;
import com.elmeftouhi.expense.notification.NotificationType;
import com.elmeftouhi.expense.paymentmethod.PaymentMethodType;
import com.elmeftouhi.expense.tenantconfig.Currency;
import com.elmeftouhi.expense.tenantconfig.Language;
import com.elmeftouhi.expense.tenantconfig.TimeFormat;
import com.elmeftouhi.expense.tenantconfig.WeekStartDay;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record UserDataBackupSnapshot(
        BackupMetadata metadata,
        TenantConfigurationEntry tenantConfiguration,
        List<ExerciceEntry> exercices,
        List<CategoryEntry> categories,
        List<PaymentMethodEntry> paymentMethods,
        List<BudgetEntry> budgets,
        List<BudgetPeriodInstanceEntry> budgetPeriodInstances,
        List<SubscriptionEntry> subscriptions,
        List<ExpenseEntry> expenses,
        List<SubscriptionPaymentEntry> subscriptionPayments,
        List<NotificationEntry> notifications,
        List<ActivityLogEntry> activityLogs
) {

    public record BackupMetadata(
            String formatVersion,
            Instant exportedAt,
            Long sourceTenantId,
            String sourceTenantName,
            Long exportedByUserId,
            String exportedByUsername
    ) {
    }

    public record TenantConfigurationEntry(
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

    public record ExerciceEntry(
            Long legacyId,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            ExerciceStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record CategoryEntry(
            Long legacyId,
            Long parentLegacyId,
            Integer level,
            String name,
            String description,
            String icon,
            String textColor,
            String backgroundColor,
            BigDecimal monthlyBudget,
            boolean active,
            boolean allowOverBudget
    ) {
    }

    public record PaymentMethodEntry(
            Long legacyId,
            String name,
            PaymentMethodType type,
            boolean isDefault,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record BudgetEntry(
            Long legacyId,
            String name,
            String description,
            BudgetPeriodicity periodicity,
            BigDecimal allocatedAmount,
            BudgetWarningType warningType,
            BigDecimal warningThreshold,
            boolean active,
            List<Long> categoryLegacyIds,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record BudgetPeriodInstanceEntry(
            Long legacyId,
            Long budgetLegacyId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal allocatedAmount,
            BigDecimal spentAmount,
            BudgetPeriodStatus status,
            BudgetWarningType warningType,
            BigDecimal warningThreshold,
            boolean warningSent,
            boolean exceededSent,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record SubscriptionEntry(
            Long legacyId,
            String name,
            String description,
            Long categoryLegacyId,
            Long ownerUserId,
            BigDecimal amount,
            String currency,
            SubscriptionFrequency frequency,
            Integer customIntervalDays,
            LocalDate nextBillingDate,
            LocalDate endDate,
            SubscriptionLifecycleStatus status,
            Integer reminderLeadDays,
            Long paymentMethodLegacyId,
            String notes,
            Integer anchorDayOfMonth,
            Integer anchorMonthOfYear,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
    }

    public record ExpenseEntry(
            Long legacyId,
            Long categoryLegacyId,
            Long userId,
            String description,
            BigDecimal amount,
            String currency,
            ExpenseType type,
            ExpenseStatus status,
            LocalDate dueDate,
            LocalDate paidDate,
            Long paymentMethodLegacyId,
            String notes,
            Boolean deleted,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
    }

    public record SubscriptionPaymentEntry(
            Long legacyId,
            Long subscriptionLegacyId,
            Long expenseLegacyId,
            LocalDate scheduledBillingDate,
            Instant createdAt
    ) {
    }

    public record NotificationEntry(
            Long legacyId,
            Long userId,
            NotificationType type,
            NotificationStatus status,
            String title,
            String message,
            String subjectType,
            Long subjectId,
            String data,
            Instant createdAt,
            Instant readAt,
            Instant archivedAt
    ) {
    }

    public record ActivityLogEntry(
            Long legacyId,
            ActivitySubjectType subjectType,
            Long subjectId,
            String subjectLabel,
            ActivityAction action,
            String actorUsername,
            String message,
            String metadata,
            Instant occurredAt
    ) {
    }
}

