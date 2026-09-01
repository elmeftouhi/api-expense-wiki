package com.elmeftouhi.expense.tenantconfig;

public enum WeekStartDay {
    MONDAY(1),
    SUNDAY(0);

    private final int value;

    WeekStartDay(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

