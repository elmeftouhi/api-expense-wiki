package com.elmeftouhi.expense.notification;

public enum NotificationChannelType {
    IN_APP("In-app notification"),
    EMAIL("Email notification"),
    SMS("SMS notification");

    private final String description;

    NotificationChannelType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

