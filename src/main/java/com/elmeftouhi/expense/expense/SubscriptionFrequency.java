package com.elmeftouhi.expense.expense;

import java.time.LocalDate;
import java.time.YearMonth;

public enum SubscriptionFrequency {
    DAILY(1),
    WEEKLY(7),
    BIWEEKLY(14),
    MONTHLY(30),
    QUARTERLY(90),
    SEMIANNUAL(180),
    YEARLY(365),
    ANNUAL(365),
    CUSTOM(0); // For custom intervals

    private final int days;

    SubscriptionFrequency(int days) {
        this.days = days;
    }

    public int getDays() {
        return days;
    }

    public LocalDate advance(LocalDate currentDate, Integer customIntervalDays, Integer anchorDayOfMonth, Integer anchorMonthOfYear) {
        return switch (this) {
            case DAILY -> currentDate.plusDays(1);
            case WEEKLY -> currentDate.plusWeeks(1);
            case BIWEEKLY -> currentDate.plusWeeks(2);
            case MONTHLY -> addMonthsAnchored(currentDate, 1, anchorDayOfMonth);
            case QUARTERLY -> addMonthsAnchored(currentDate, 3, anchorDayOfMonth);
            case SEMIANNUAL -> addMonthsAnchored(currentDate, 6, anchorDayOfMonth);
            case YEARLY, ANNUAL -> addYearsAnchored(currentDate, anchorDayOfMonth, anchorMonthOfYear);
            case CUSTOM -> {
                if (customIntervalDays == null || customIntervalDays <= 0) {
                    throw new IllegalArgumentException("customIntervalDays must be greater than 0 for CUSTOM frequency");
                }
                yield currentDate.plusDays(customIntervalDays);
            }
        };
    }

    private static LocalDate addMonthsAnchored(LocalDate currentDate, int monthsToAdd, Integer anchorDayOfMonth) {
        int day = anchorDayOfMonth != null ? anchorDayOfMonth : currentDate.getDayOfMonth();
        LocalDate targetMonth = currentDate.plusMonths(monthsToAdd).withDayOfMonth(1);
        int adjustedDay = Math.min(day, YearMonth.from(targetMonth).lengthOfMonth());
        return targetMonth.withDayOfMonth(adjustedDay);
    }

    private static LocalDate addYearsAnchored(LocalDate currentDate, Integer anchorDayOfMonth, Integer anchorMonthOfYear) {
        int month = anchorMonthOfYear != null ? anchorMonthOfYear : currentDate.getMonthValue();
        int day = anchorDayOfMonth != null ? anchorDayOfMonth : currentDate.getDayOfMonth();
        int year = currentDate.plusYears(1).getYear();
        YearMonth targetYearMonth = YearMonth.of(year, month);
        int adjustedDay = Math.min(day, targetYearMonth.lengthOfMonth());
        return LocalDate.of(year, month, adjustedDay);
    }
}

