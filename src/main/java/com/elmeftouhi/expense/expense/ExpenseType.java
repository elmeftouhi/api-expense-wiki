package com.elmeftouhi.expense.expense;

public enum ExpenseType {
    NORMAL("One-time expense"),
    SUBSCRIPTION("Recurring subscription");

    private final String description;

    ExpenseType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

