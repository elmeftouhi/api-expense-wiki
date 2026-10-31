package com.elmeftouhi.expense.datatransfer.dto;

public record ImportUserDataResponse(
        Long tenantId,
        String tenantName,
        boolean switchedDefaultTenant,
        ImportCounts counts
) {
    public record ImportCounts(
            int exercices,
            int categories,
            int paymentMethods,
            int budgets,
            int budgetPeriodInstances,
            int subscriptions,
            int expenses,
            int subscriptionPayments,
            int notifications,
            int activityLogs
    ) {
    }
}

