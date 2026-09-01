package com.elmeftouhi.expense.tenantconfig;

public enum TimeFormat {
    FORMAT_12H("12H"),
    FORMAT_24H("24H");

    private final String value;

    TimeFormat(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

