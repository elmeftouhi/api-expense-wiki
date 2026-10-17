package com.elmeftouhi.expense.expense;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SubscriptionFrequencyTests {

    @Test
    void monthly_usesLastDayForShortMonthsWithoutDriftingAnchor() {
        LocalDate jan31 = LocalDate.of(2026, 1, 31);
        LocalDate febDate = SubscriptionFrequency.MONTHLY.advance(jan31, null, 31, 1);
        LocalDate marDate = SubscriptionFrequency.MONTHLY.advance(febDate, null, 31, 1);

        assertEquals(LocalDate.of(2026, 2, 28), febDate);
        assertEquals(LocalDate.of(2026, 3, 31), marDate);
    }

    @Test
    void annual_handlesLeapDayByClampingToLastDayOfMonth() {
        LocalDate leapDay = LocalDate.of(2024, 2, 29);
        LocalDate nextYear = SubscriptionFrequency.ANNUAL.advance(leapDay, null, 29, 2);

        assertEquals(LocalDate.of(2025, 2, 28), nextYear);
    }
}

