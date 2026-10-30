package com.elmeftouhi.expense.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.elmeftouhi.expense.budget.repository.BudgetPeriodInstanceRepository;
import com.elmeftouhi.expense.dashboard.dto.DashboardSummaryResponse;
import com.elmeftouhi.expense.dashboard.service.DashboardService;
import com.elmeftouhi.expense.expense.ExpenseStatus;
import com.elmeftouhi.expense.expense.entity.Expense;
import com.elmeftouhi.expense.expense.repository.ExpenseRepository;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Tests")
class DashboardServiceTests {

    private static final Long TENANT_ID = 1L;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private BudgetPeriodInstanceRepository budgetPeriodInstanceRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @Test
    @DisplayName("Should return dashboard summary with computed percentages and counts")
    void testGetSummary() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth previousMonth = currentMonth.minusMonths(1);
        Set<ExpenseStatus> trackedStatuses = EnumSet.of(ExpenseStatus.PAID, ExpenseStatus.PENDING, ExpenseStatus.OVERDUE);

        when(expenseRepository.sumByTenantAndStatusesAndDueDateRange(
                TENANT_ID, trackedStatuses, currentMonth.atDay(1), currentMonth.atEndOfMonth()))
                .thenReturn(BigDecimal.valueOf(250));
        when(expenseRepository.sumByTenantAndStatusesAndDueDateRange(
                TENANT_ID, trackedStatuses, previousMonth.atDay(1), previousMonth.atEndOfMonth()))
                .thenReturn(BigDecimal.valueOf(200));
        when(expenseRepository.sumByTenantAndStatusesAndDueDateRange(
                TENANT_ID, trackedStatuses, LocalDate.of(today.getYear(), 1, 1), LocalDate.of(today.getYear(), 12, 31)))
                .thenReturn(BigDecimal.valueOf(1000));
        when(expenseRepository.sumByTenantAndStatusesAndDueDateRange(
                TENANT_ID, trackedStatuses, LocalDate.of(today.getYear() - 1, 1, 1), LocalDate.of(today.getYear() - 1, 12, 31)))
                .thenReturn(BigDecimal.valueOf(800));

        when(expenseRepository.sumByTenantAndStatusAndDateRange(
                TENANT_ID, ExpenseStatus.PENDING, today, currentMonth.atEndOfMonth()))
                .thenReturn(BigDecimal.valueOf(120));
        when(expenseRepository.countByTenantIdAndStatusAndDueDateBetweenAndDeletedFalse(
                TENANT_ID, ExpenseStatus.PENDING, today, currentMonth.atEndOfMonth()))
                .thenReturn(3L);

        Expense nextUpcoming = new Expense();
        nextUpcoming.setId(99L);
        nextUpcoming.setDescription("Netflix");
        when(expenseRepository.findFirstByTenantIdAndStatusAndDueDateBetweenAndDeletedFalseOrderByDueDateAsc(
                TENANT_ID, ExpenseStatus.PENDING, today, currentMonth.atEndOfMonth()))
                .thenReturn(java.util.Optional.of(nextUpcoming));

        when(expenseRepository.sumByTenantAndStatusesAndDueDateBefore(
                TENANT_ID, EnumSet.of(ExpenseStatus.PENDING, ExpenseStatus.OVERDUE), today))
                .thenReturn(BigDecimal.valueOf(70));
        when(expenseRepository.countByTenantAndStatusesAndDueDateBefore(
                TENANT_ID, EnumSet.of(ExpenseStatus.PENDING, ExpenseStatus.OVERDUE), today))
                .thenReturn(2L);

        when(expenseRepository.sumByTenantAndStatusAndDueDateOnOrAfter(TENANT_ID, ExpenseStatus.PENDING, today))
                .thenReturn(BigDecimal.valueOf(220));
        when(expenseRepository.countByTenantAndStatusAndDueDateOnOrAfter(TENANT_ID, ExpenseStatus.PENDING, today))
                .thenReturn(5L);

        BudgetPeriodInstanceRepository.CurrentBudgetTotalsView totals = new BudgetPeriodInstanceRepository.CurrentBudgetTotalsView() {
            @Override
            public BigDecimal getAllocatedAmount() {
                return BigDecimal.valueOf(1000);
            }

            @Override
            public BigDecimal getSpentAmount() {
                return BigDecimal.valueOf(400);
            }
        };
        when(budgetPeriodInstanceRepository.getCurrentTotals(TENANT_ID, today)).thenReturn(totals);

        DashboardSummaryResponse response = dashboardService.getSummary();

        assertEquals(BigDecimal.valueOf(250), response.monthExpenses().currentTotal());
        assertEquals(BigDecimal.valueOf(200), response.monthExpenses().previousTotal());
        assertEquals(BigDecimal.valueOf(25.00).setScale(2), response.monthExpenses().percentageChange());

        assertEquals(BigDecimal.valueOf(1000), response.yearExpenses().currentTotal());
        assertEquals(BigDecimal.valueOf(800), response.yearExpenses().previousTotal());
        assertEquals(BigDecimal.valueOf(25.00).setScale(2), response.yearExpenses().percentageChange());

        assertEquals(BigDecimal.valueOf(120), response.upcomingThisMonth().totalAmount());
        assertEquals(3L, response.upcomingThisMonth().count());
        assertEquals("Netflix", response.upcomingThisMonth().nextExpenseLabel());

        assertEquals(BigDecimal.valueOf(70), response.overdue().totalAmount());
        assertEquals(2L, response.overdue().count());

        assertEquals(BigDecimal.valueOf(220), response.pending().totalAmount());
        assertEquals(5L, response.pending().count());

        assertEquals(BigDecimal.valueOf(1000), response.monthlyBudget().totalBudget());
        assertEquals(BigDecimal.valueOf(40.00).setScale(2), response.monthlyBudget().usedPercentage());
        assertEquals(BigDecimal.valueOf(600), response.monthlyBudget().totalRemaining());
    }

    @Test
    @DisplayName("Should return zero percentages and null next label when there is no data")
    void testGetSummaryWithoutData() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth previousMonth = currentMonth.minusMonths(1);
        Set<ExpenseStatus> trackedStatuses = EnumSet.of(ExpenseStatus.PAID, ExpenseStatus.PENDING, ExpenseStatus.OVERDUE);

        when(expenseRepository.sumByTenantAndStatusesAndDueDateRange(
                TENANT_ID, trackedStatuses, currentMonth.atDay(1), currentMonth.atEndOfMonth()))
                .thenReturn(BigDecimal.ZERO);
        when(expenseRepository.sumByTenantAndStatusesAndDueDateRange(
                TENANT_ID, trackedStatuses, previousMonth.atDay(1), previousMonth.atEndOfMonth()))
                .thenReturn(BigDecimal.ZERO);
        when(expenseRepository.sumByTenantAndStatusesAndDueDateRange(
                TENANT_ID, trackedStatuses, LocalDate.of(today.getYear(), 1, 1), LocalDate.of(today.getYear(), 12, 31)))
                .thenReturn(BigDecimal.ZERO);
        when(expenseRepository.sumByTenantAndStatusesAndDueDateRange(
                TENANT_ID, trackedStatuses, LocalDate.of(today.getYear() - 1, 1, 1), LocalDate.of(today.getYear() - 1, 12, 31)))
                .thenReturn(BigDecimal.ZERO);

        when(expenseRepository.sumByTenantAndStatusAndDateRange(
                TENANT_ID, ExpenseStatus.PENDING, today, currentMonth.atEndOfMonth()))
                .thenReturn(BigDecimal.ZERO);
        when(expenseRepository.countByTenantIdAndStatusAndDueDateBetweenAndDeletedFalse(
                TENANT_ID, ExpenseStatus.PENDING, today, currentMonth.atEndOfMonth()))
                .thenReturn(0L);
        when(expenseRepository.findFirstByTenantIdAndStatusAndDueDateBetweenAndDeletedFalseOrderByDueDateAsc(
                TENANT_ID, ExpenseStatus.PENDING, today, currentMonth.atEndOfMonth()))
                .thenReturn(java.util.Optional.empty());

        when(expenseRepository.sumByTenantAndStatusesAndDueDateBefore(
                TENANT_ID, EnumSet.of(ExpenseStatus.PENDING, ExpenseStatus.OVERDUE), today))
                .thenReturn(BigDecimal.ZERO);
        when(expenseRepository.countByTenantAndStatusesAndDueDateBefore(
                TENANT_ID, EnumSet.of(ExpenseStatus.PENDING, ExpenseStatus.OVERDUE), today))
                .thenReturn(0L);

        when(expenseRepository.sumByTenantAndStatusAndDueDateOnOrAfter(TENANT_ID, ExpenseStatus.PENDING, today))
                .thenReturn(BigDecimal.ZERO);
        when(expenseRepository.countByTenantAndStatusAndDueDateOnOrAfter(TENANT_ID, ExpenseStatus.PENDING, today))
                .thenReturn(0L);

        BudgetPeriodInstanceRepository.CurrentBudgetTotalsView totals = new BudgetPeriodInstanceRepository.CurrentBudgetTotalsView() {
            @Override
            public BigDecimal getAllocatedAmount() {
                return BigDecimal.ZERO;
            }

            @Override
            public BigDecimal getSpentAmount() {
                return BigDecimal.ZERO;
            }
        };
        when(budgetPeriodInstanceRepository.getCurrentTotals(TENANT_ID, today)).thenReturn(totals);

        DashboardSummaryResponse response = dashboardService.getSummary();

        assertEquals(BigDecimal.ZERO, response.monthExpenses().percentageChange());
        assertEquals(BigDecimal.ZERO, response.yearExpenses().percentageChange());
        assertEquals(BigDecimal.ZERO, response.monthlyBudget().usedPercentage());
        assertNull(response.upcomingThisMonth().nextExpenseLabel());
    }
}

