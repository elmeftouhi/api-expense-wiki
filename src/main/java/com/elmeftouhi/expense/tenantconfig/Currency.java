package com.elmeftouhi.expense.tenantconfig;

public enum Currency {
    USD("$", 2),
    EUR("€", 2),
    GBP("£", 2),
    CAD("C$", 2),
    AUD("A$", 2),
    JPY("¥", 0),
    CNY("¥", 2),
    INR("₹", 2),
    SAR("﷼", 2),
    AED("د.إ", 2);

    private final String symbol;
    private final int defaultDecimals;

    Currency(String symbol, int defaultDecimals) {
        this.symbol = symbol;
        this.defaultDecimals = defaultDecimals;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getDefaultDecimals() {
        return defaultDecimals;
    }
}

