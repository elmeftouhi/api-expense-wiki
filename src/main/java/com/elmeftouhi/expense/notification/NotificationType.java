package com.elmeftouhi.expense.notification;

public enum NotificationType {
    EXPENSE_CREATED("A new expense has been created"),
    EXPENSE_DUE_TODAY("An expense is due today"),
    EXPENSE_OVERDUE("An expense is overdue"),
    EXPENSE_DUE_SOON("An expense is due soon"),
    SUBSCRIPTION_NEXT("Next subscription payment upcoming"),
    PAYMENT_REMINDER("Payment reminder"),
    BUDGET_EXCEEDED("Category budget exceeded"),
    BUDGET_ALERT("Budget alert - approaching limit");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

