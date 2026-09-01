package com.elmeftouhi.expense.expense;

public enum ExpenseStatus {
    PENDING("Not yet paid"),
    PAID("Successfully paid"),
    OVERDUE("Past due date, not paid"),
    CANCELLED("Cancelled or no longer active");

    private final String description;

    ExpenseStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

