package com.elmeftouhi.expense.expense;

public enum SubscriptionFrequency {
    DAILY(1),
    WEEKLY(7),
    BIWEEKLY(14),
    MONTHLY(30),
    QUARTERLY(90),
    SEMIANNUAL(180),
    YEARLY(365),
    CUSTOM(0); // For custom intervals

    private final int days;

    SubscriptionFrequency(int days) {
        this.days = days;
    }

    public int getDays() {
        return days;
    }
}

