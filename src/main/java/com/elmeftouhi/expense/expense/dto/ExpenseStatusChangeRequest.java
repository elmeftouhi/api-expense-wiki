package com.elmeftouhi.expense.expense.dto;

import com.elmeftouhi.expense.expense.ExpenseStatus;
import java.time.LocalDate;

public record ExpenseStatusChangeRequest(
        ExpenseStatus status,
        LocalDate paidDate
) {
}

