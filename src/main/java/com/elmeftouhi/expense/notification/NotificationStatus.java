package com.elmeftouhi.expense.notification;

public enum NotificationStatus {
    SENT("Notification sent"),
    READ("Notification read by user"),
    ARCHIVED("Notification archived");

    private final String description;

    NotificationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

