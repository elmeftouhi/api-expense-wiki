package com.elmeftouhi.expense.budget;

import com.elmeftouhi.expense.budget.entity.ExpenseBudgetSnapshot;
import com.elmeftouhi.expense.budget.entity.BudgetPeriodInstance;
import com.elmeftouhi.expense.budget.repository.BudgetRepository;
import com.elmeftouhi.expense.budget.repository.BudgetPeriodInstanceRepository;
import com.elmeftouhi.expense.budget.service.BudgetTrackingService;
import com.elmeftouhi.expense.budget.entity.Budget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elmeftouhi.expense.category.entity.Category;
import com.elmeftouhi.expense.expense.ExpenseStatus;
import com.elmeftouhi.expense.notification.service.NotificationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetTrackingServiceTests {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private BudgetPeriodInstanceRepository budgetPeriodInstanceRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BudgetTrackingService budgetTrackingService;

    @Test
    @DisplayName("applyExpenseCreated lazily creates period and deducts amount")
    void applyExpenseCreated_createsPeriodAndAddsSpent() {
        Long tenantId = 1L;
        Long categoryId = 5L;

        Budget budget = buildBudget(10L, tenantId, categoryId);
        ExpenseBudgetSnapshot snapshot = new ExpenseBudgetSnapshot(
                100L,
                tenantId,
                9L,
                categoryId,
                BigDecimal.valueOf(50),
                ExpenseStatus.PAID,
                LocalDate.of(2026, 10, 14)
        );

        LocalDate startDate = LocalDate.of(2026, 10, 1);

        when(budgetRepository.findActiveByTenantIdAndCategoryId(tenantId, categoryId)).thenReturn(List.of(budget));
        when(budgetPeriodInstanceRepository.findByBudgetIdAndTenantIdAndStartDate(budget.getId(), tenantId, startDate))
                .thenReturn(Optional.empty());
        when(budgetPeriodInstanceRepository.findTopByBudgetIdAndTenantIdAndStartDateBeforeOrderByStartDateDesc(
                budget.getId(), tenantId, startDate))
                .thenReturn(Optional.empty());
        when(budgetPeriodInstanceRepository.save(any(BudgetPeriodInstance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        budgetTrackingService.applyExpenseCreated(snapshot);

        verify(budgetPeriodInstanceRepository, atLeastOnce()).save(any(BudgetPeriodInstance.class));
        verify(notificationService, never()).sendNotification(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("applyExpenseUpdated skips missing historical period and applies paid current snapshot")
    void applyExpenseUpdated_skipsMissingHistoricalPeriod() {
        Long tenantId = 1L;

        ExpenseBudgetSnapshot previous = new ExpenseBudgetSnapshot(
                101L,
                tenantId,
                9L,
                99L,
                BigDecimal.valueOf(30),
                ExpenseStatus.PAID,
                LocalDate.of(2026, 9, 1)
        );

        ExpenseBudgetSnapshot current = new ExpenseBudgetSnapshot(
                101L,
                tenantId,
                9L,
                5L,
                BigDecimal.valueOf(30),
                ExpenseStatus.PAID,
                LocalDate.of(2026, 9, 1)
        );

        Budget budget = buildBudget(10L, tenantId, 5L);
        LocalDate startDate = LocalDate.of(2026, 9, 1);

        when(budgetRepository.findActiveByTenantIdAndCategoryId(tenantId, 99L)).thenReturn(List.of());
        when(budgetRepository.findActiveByTenantIdAndCategoryId(tenantId, 5L)).thenReturn(List.of(budget));
        when(budgetPeriodInstanceRepository.findByBudgetIdAndTenantIdAndStartDate(budget.getId(), tenantId, startDate))
                .thenReturn(Optional.empty());

        budgetTrackingService.applyExpenseUpdated(previous, current);

        verify(budgetPeriodInstanceRepository, never()).save(any(BudgetPeriodInstance.class));
    }

    @Test
    @DisplayName("applyExpenseDeleted recalculates and reopens period status")
    void applyExpenseDeleted_recalculatesSpentAndStatus() {
        Long tenantId = 1L;
        Long categoryId = 5L;

        Budget budget = buildBudget(10L, tenantId, categoryId);
        BudgetPeriodInstance periodInstance = new BudgetPeriodInstance();
        periodInstance.assignTenantId(tenantId);
        periodInstance.setBudget(budget);
        periodInstance.setStartDate(LocalDate.of(2026, 10, 1));
        periodInstance.setEndDate(LocalDate.of(2026, 10, 31));
        periodInstance.setAllocatedAmount(BigDecimal.valueOf(100));
        periodInstance.setSpentAmount(BigDecimal.valueOf(120));
        periodInstance.setStatus(BudgetPeriodStatus.CLOSED);
        periodInstance.setWarningType(BudgetWarningType.AMOUNT);
        periodInstance.setWarningThreshold(BigDecimal.valueOf(100));
        periodInstance.setWarningSent(true);
        periodInstance.setExceededSent(true);

        ExpenseBudgetSnapshot snapshot = new ExpenseBudgetSnapshot(
                100L,
                tenantId,
                9L,
                categoryId,
                BigDecimal.valueOf(30),
                ExpenseStatus.PAID,
                LocalDate.now()
        );

        when(budgetRepository.findActiveByTenantIdAndCategoryId(tenantId, categoryId)).thenReturn(List.of(budget));
        when(budgetPeriodInstanceRepository.findByBudgetIdAndTenantIdAndStartDate(
                budget.getId(), tenantId, LocalDate.now().withDayOfMonth(1)))
                .thenReturn(Optional.of(periodInstance));
        when(budgetPeriodInstanceRepository.save(any(BudgetPeriodInstance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        budgetTrackingService.applyExpenseDeleted(snapshot);

        assertEquals(BigDecimal.valueOf(90), periodInstance.getSpentAmount());
        assertEquals(BudgetPeriodStatus.OPEN, periodInstance.getStatus());
        assertFalse(periodInstance.isWarningSent());
        assertFalse(periodInstance.isExceededSent());
    }

    @Test
    @DisplayName("applyExpenseCreated ignores non-paid expenses")
    void applyExpenseCreated_ignoresNonPaid() {
        ExpenseBudgetSnapshot snapshot = new ExpenseBudgetSnapshot(
                100L,
                1L,
                9L,
                5L,
                BigDecimal.valueOf(30),
                ExpenseStatus.PENDING,
                LocalDate.now()
        );

        budgetTrackingService.applyExpenseCreated(snapshot);

        verify(budgetRepository, never()).findActiveByTenantIdAndCategoryId(any(), any());
    }


    private Budget buildBudget(Long budgetId, Long tenantId, Long categoryId) {
        Category category = new Category();
        category.setId(categoryId);

        Budget budget = new Budget();
        budget.setId(budgetId);
        budget.assignTenantId(tenantId);
        budget.setName("Main budget");
        budget.setPeriodicity(BudgetPeriodicity.MONTHLY);
        budget.setAllocatedAmount(BigDecimal.valueOf(200));
        budget.setWarningType(BudgetWarningType.PERCENTAGE);
        budget.setWarningThreshold(BigDecimal.valueOf(80));
        budget.setCategories(Set.of(category));
        budget.setActive(true);
        return budget;
    }
}

