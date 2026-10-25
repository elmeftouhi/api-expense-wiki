package com.elmeftouhi.expense.expense;

import com.elmeftouhi.expense.expense.entity.Expense;
import com.elmeftouhi.expense.expense.repository.ExpenseRepository;
import com.elmeftouhi.expense.expense.service.ExpenseService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.elmeftouhi.expense.activity.service.ActivityLogService;
import com.elmeftouhi.expense.budget.service.BudgetTrackingService;
import com.elmeftouhi.expense.category.entity.Category;
import com.elmeftouhi.expense.category.repository.CategoryRepository;
import com.elmeftouhi.expense.expense.dto.ExpenseCreateRequest;
import com.elmeftouhi.expense.expense.dto.ExpenseDailyChartResponse;
import com.elmeftouhi.expense.expense.dto.ExpenseMonthlyChartResponse;
import com.elmeftouhi.expense.expense.dto.ExpenseResponse;
import com.elmeftouhi.expense.expense.dto.ExpenseStatisticsResponse;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.paymentmethod.entity.PaymentMethod;
import com.elmeftouhi.expense.paymentmethod.service.PaymentMethodService;
import com.elmeftouhi.expense.security.JwtPrincipal;
import com.elmeftouhi.expense.shared.exception.BadRequestException;
import com.elmeftouhi.expense.shared.exception.ResourceNotFoundException;
import com.elmeftouhi.expense.tenantconfig.ConfigurationContext;
import com.elmeftouhi.expense.tenantconfig.Currency;
import com.elmeftouhi.expense.tenantconfig.entity.TenantConfiguration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService Tests")
class ExpenseServiceTests {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private BudgetTrackingService budgetTrackingService;

    @Mock
    private PaymentMethodService paymentMethodService;

    @InjectMocks
    private ExpenseService expenseService;

    private Long tenantId = 1L;
    private Long categoryId = 1L;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(tenantId);

        JwtPrincipal principal = new JwtPrincipal(userId, "test-user", tenantId, Set.of(tenantId));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null)
        );

        TenantConfiguration config = new TenantConfiguration();
        config.setCurrency(Currency.USD);
        config.setDecimalPlaces(2);
        ConfigurationContext.setConfiguration(config);
    }

    @Test
    @DisplayName("Should create a new expense successfully")
    void testCreateExpense() {
        // Given
        ExpenseCreateRequest request = new ExpenseCreateRequest(
                categoryId,
                "Test Expense",
                BigDecimal.valueOf(100.00),
                LocalDate.of(2026, 9, 30),
                null,
                ExpenseType.NORMAL,
                3L,
                "Test notes"
        );

        Category category = new Category();
        category.setId(categoryId);
        category.setName("Food");

        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setId(3L);
        paymentMethod.setName("Cash");
        paymentMethod.setActive(true);

        Expense savedExpense = new Expense();
        savedExpense.setId(1L);
        savedExpense.setCategory(category);
        savedExpense.setDescription(request.description());
        savedExpense.setAmount(request.amount());
        savedExpense.setCurrency("USD");
        savedExpense.setType(ExpenseType.NORMAL);
        savedExpense.setStatus(ExpenseStatus.PENDING);
        savedExpense.setDueDate(request.dueDate());
        savedExpense.setPaidDate(request.paidDate());

        when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(java.util.Optional.of(category));
        when(paymentMethodService.resolveForUsage(tenantId, 3L)).thenReturn(paymentMethod);
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

        // When
        ExpenseResponse response = expenseService.create(request);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Test Expense", response.description());
        assertEquals(BigDecimal.valueOf(100.00), response.amount());
        assertEquals(ExpenseStatus.PENDING, response.status());
        assertNull(response.paidDate());

        verify(expenseRepository, times(1)).save(any(Expense.class));
        verify(activityLogService, times(1)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when creating expense with invalid category")
    void testCreateExpenseWithInvalidCategory() {
        // Given
        ExpenseCreateRequest request = new ExpenseCreateRequest(
                999L,
                "Test Expense",
                BigDecimal.valueOf(100.00),
                LocalDate.of(2026, 9, 30),
                null,
                ExpenseType.NORMAL,
                3L,
                "Test notes"
        );

        when(categoryRepository.findByIdAndTenantId(999L, tenantId)).thenReturn(java.util.Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> expenseService.create(request));
    }

    @Test
    @DisplayName("Should allow creating expense without paidDate")
    void testCreateExpenseWithoutPaidDate() {
        // Given
        ExpenseCreateRequest request = new ExpenseCreateRequest(
                categoryId,
                "Test Expense",
                BigDecimal.valueOf(100.00),
                LocalDate.of(2026, 9, 30),
                null,
                ExpenseType.NORMAL,
                3L,
                "Test notes"
        );

        Category category = new Category();
        category.setId(categoryId);
        category.setName("Food");

        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setId(3L);
        paymentMethod.setName("Cash");
        paymentMethod.setActive(true);

        Expense savedExpense = new Expense();
        savedExpense.setId(2L);
        savedExpense.setCategory(category);
        savedExpense.setDescription(request.description());
        savedExpense.setAmount(request.amount());
        savedExpense.setCurrency("USD");
        savedExpense.setType(ExpenseType.NORMAL);
        savedExpense.setStatus(ExpenseStatus.PENDING);
        savedExpense.setDueDate(request.dueDate());
        savedExpense.setPaidDate(null);

        when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(java.util.Optional.of(category));
        when(paymentMethodService.resolveForUsage(tenantId, 3L)).thenReturn(paymentMethod);
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

        // When
        ExpenseResponse response = expenseService.create(request);

        // Then
        assertNotNull(response);
        assertEquals(ExpenseStatus.PENDING, response.status());
        assertNull(response.paidDate());
    }

    @Test
    @DisplayName("Should reject creating subscription definitions through expense API")
    void testRejectSubscriptionTypeFromExpenseCreate() {
        ExpenseCreateRequest request = new ExpenseCreateRequest(
                categoryId,
                "Template",
                BigDecimal.valueOf(15),
                LocalDate.of(2026, 10, 31),
                null,
                ExpenseType.SUBSCRIPTION,
                3L,
                null
        );

        Category category = new Category();
        category.setId(categoryId);
        when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(java.util.Optional.of(category));
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setId(3L);
        paymentMethod.setName("Cash");
        paymentMethod.setActive(true);
        when(paymentMethodService.resolveForUsage(tenantId, 3L)).thenReturn(paymentMethod);

        assertThrows(BadRequestException.class, () -> expenseService.create(request));
        verify(expenseRepository, never()).save(any(Expense.class));
    }

    @Test
    @DisplayName("Should mark expense as paid")
    void testMarkAsPaid() {
        // Given
        Expense expense = new Expense();
        expense.setId(1L);
        expense.setStatus(ExpenseStatus.PENDING);
        expense.setDescription("Test");
        expense.setAmount(BigDecimal.valueOf(100));
        expense.setCurrency("USD");
        expense.setCategory(new Category());
        expense.assignTenantId(tenantId);

        LocalDate paidDate = LocalDate.of(2026, 9, 15);

        when(expenseRepository.findByIdAndTenantId(1L, tenantId))
                .thenReturn(java.util.Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);

        // When
        ExpenseResponse response = expenseService.markAsPaid(1L, paidDate);

        // Then
        assertNotNull(response);
        assertEquals(paidDate, response.paidDate());
        assertEquals(ExpenseStatus.PAID, response.status());

        verify(expenseRepository, times(1)).save(any(Expense.class));
        verify(budgetTrackingService, times(1)).applyExpenseUpdated(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when marking already paid expense as paid")
    void testMarkAlreadyPaidExpenseAsPaid() {
        // Given
        Expense expense = new Expense();
        expense.setId(1L);
        expense.setStatus(ExpenseStatus.PAID);

        when(expenseRepository.findByIdAndTenantId(1L, tenantId))
                .thenReturn(java.util.Optional.of(expense));

        // When & Then
        assertThrows(BadRequestException.class,
                () -> expenseService.markAsPaid(1L, LocalDate.now()));
    }

    @Test
    @DisplayName("Should require paidDate when changing status to PAID")
    void testChangeStatusRequiresPaidDateWhenPaid() {
        // Given
        Expense expense = new Expense();
        expense.setId(1L);
        expense.setStatus(ExpenseStatus.PENDING);
        expense.setDescription("Test");
        expense.setAmount(BigDecimal.valueOf(100));
        expense.setCurrency("USD");
        Category category = new Category();
        category.setId(categoryId);
        category.setName("Food");
        expense.setCategory(category);
        expense.assignTenantId(tenantId);

        when(expenseRepository.findByIdAndTenantId(1L, tenantId))
                .thenReturn(java.util.Optional.of(expense));

        // When & Then
        assertThrows(BadRequestException.class,
                () -> expenseService.changeStatus(1L, ExpenseStatus.PAID, null));
    }

    @Test
    @DisplayName("Should change status and update budget tracking")
    void testChangeStatusUpdatesBudgetTracking() {
        // Given
        Category category = new Category();
        category.setId(categoryId);
        category.setName("Food");

        Expense expense = new Expense();
        expense.setId(1L);
        expense.setStatus(ExpenseStatus.PENDING);
        expense.setDescription("Test");
        expense.setAmount(BigDecimal.valueOf(100));
        expense.setCurrency("USD");
        expense.setCategory(category);
        expense.setDueDate(LocalDate.of(2026, 9, 10));
        expense.assignTenantId(tenantId);

        when(expenseRepository.findByIdAndTenantId(1L, tenantId))
                .thenReturn(java.util.Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);

        // When
        ExpenseResponse response = expenseService.changeStatus(1L, ExpenseStatus.OVERDUE, null);

        // Then
        assertNotNull(response);
        assertEquals(ExpenseStatus.OVERDUE, response.status());
        verify(budgetTrackingService, times(1)).applyExpenseUpdated(any(), any());
    }

    @Test
    @DisplayName("Should delete an expense (soft delete)")
    void testDeleteExpense() {
        // Given
        Expense expense = new Expense();
        expense.setId(1L);
        expense.setDeleted(false);
        expense.setDescription("Test");
        expense.setAmount(BigDecimal.valueOf(100));
        expense.setCurrency("USD");
        expense.setCategory(new Category());
        expense.assignTenantId(tenantId);

        when(expenseRepository.findByIdAndTenantId(1L, tenantId))
                .thenReturn(java.util.Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);

        // When
        expenseService.delete(1L);

        // Then
        verify(expenseRepository, times(1)).save(any(Expense.class));
        assertTrue(expense.getDeleted());
    }

    @Test
    @DisplayName("Should find overdue expenses")
    void testFindOverdueExpenses() {
        // Given
        Category category = new Category();
        category.setId(categoryId);
        category.setName("Food");

        Expense overdueExpense = new Expense();
        overdueExpense.setId(1L);
        overdueExpense.setCategory(category);
        overdueExpense.setStatus(ExpenseStatus.PENDING);
        overdueExpense.setDueDate(LocalDate.of(2026, 8, 15));
        overdueExpense.assignTenantId(tenantId);

        when(expenseRepository.findByTenantIdAndStatusAndDueDateBeforeAndDeletedFalse(
                tenantId, ExpenseStatus.PENDING, LocalDate.now()))
                .thenReturn(java.util.List.of(overdueExpense));

        // When
        var results = expenseService.findOverdue();

        // Then
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @Test
    @DisplayName("Should build hierarchical statistics with parent totals propagated to subcategories")
    void testGetStatisticsWithSubCategoryRollup() {
        // Given
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 31);

        Category root = new Category();
        root.setId(1L);
        root.setName("Food");

        Category groceries = new Category();
        groceries.setId(2L);
        groceries.setName("Groceries");

        Category restaurants = new Category();
        restaurants.setId(3L);
        restaurants.setName("Restaurants");

        when(expenseRepository.sumByTenantAndStatusAndDateRange(tenantId, ExpenseStatus.PENDING, startDate, endDate))
                .thenReturn(BigDecimal.valueOf(10));
        when(expenseRepository.sumByTenantAndStatusAndDateRange(tenantId, ExpenseStatus.PAID, startDate, endDate))
                .thenReturn(BigDecimal.valueOf(20));
        when(expenseRepository.sumByTenantAndStatusAndDateRange(tenantId, ExpenseStatus.OVERDUE, startDate, endDate))
                .thenReturn(BigDecimal.valueOf(5));

        when(expenseRepository.findByTenantIdAndStatusAndDeletedFalse(tenantId, ExpenseStatus.PENDING))
                .thenReturn(List.of(new Expense(), new Expense()));
        when(expenseRepository.findByTenantIdAndStatusAndDeletedFalse(tenantId, ExpenseStatus.OVERDUE))
                .thenReturn(List.of(new Expense()));

        when(categoryRepository.findRootCategories(tenantId, false)).thenReturn(List.of(root));
        when(categoryRepository.findChildCategories(tenantId, 1L, false)).thenReturn(List.of(groceries, restaurants));
        when(categoryRepository.findChildCategories(tenantId, 2L, false)).thenReturn(List.of());
        when(categoryRepository.findChildCategories(tenantId, 3L, false)).thenReturn(List.of());

        when(expenseRepository.sumByCategoryAndDateRange(tenantId, 2L, startDate, endDate))
                .thenReturn(BigDecimal.valueOf(7));
        when(expenseRepository.sumByCategoryAndDateRange(tenantId, 3L, startDate, endDate))
                .thenReturn(BigDecimal.valueOf(3));

        // When
        ExpenseStatisticsResponse response = expenseService.getStatistics(startDate, endDate);

        // Then
        assertEquals(BigDecimal.valueOf(35), response.totalExpenses());
        assertEquals(1, response.byCategory().size());

        ExpenseStatisticsResponse.CategorySummary parentSummary = response.byCategory().get(0);
        assertEquals(1L, parentSummary.categoryId());
        assertEquals("Food", parentSummary.categoryName());
        assertEquals(BigDecimal.valueOf(10), parentSummary.total());
        assertNull(parentSummary.parentTotal());
        assertEquals(2, parentSummary.subCategories().size());

        ExpenseStatisticsResponse.CategorySummary firstSubCategory = parentSummary.subCategories().get(0);
        ExpenseStatisticsResponse.CategorySummary secondSubCategory = parentSummary.subCategories().get(1);
        assertEquals(BigDecimal.valueOf(10), firstSubCategory.parentTotal());
        assertEquals(BigDecimal.valueOf(10), secondSubCategory.parentTotal());

        verify(expenseRepository, never()).sumByCategoryAndDateRange(tenantId, 1L, startDate, endDate);
    }

    @Test
    @DisplayName("Should return all days of month and fill missing daily totals with zero")
    void testGetDailyTotalsForMonthWithZeroFill() {
        Expense first = new Expense();
        first.setDueDate(LocalDate.of(2026, 10, 1));
        first.setAmount(BigDecimal.valueOf(12));

        Expense second = new Expense();
        second.setDueDate(LocalDate.of(2026, 10, 3));
        second.setAmount(BigDecimal.valueOf(8));

        Expense third = new Expense();
        third.setDueDate(LocalDate.of(2026, 10, 3));
        third.setAmount(BigDecimal.valueOf(2));

        when(expenseRepository.findByTenantIdAndStatusAndDueDateBetweenAndDeletedFalse(
                tenantId, ExpenseStatus.PAID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(first, second, third));

        ExpenseDailyChartResponse response = expenseService.getDailyTotalsForMonth(2026, 10);

        assertEquals(31, response.totals().size());
        assertEquals(BigDecimal.valueOf(12), response.totals().get(0).total());
        assertEquals(BigDecimal.ZERO, response.totals().get(1).total());
        assertEquals(BigDecimal.valueOf(10), response.totals().get(2).total());
        assertEquals(BigDecimal.ZERO, response.totals().get(30).total());
    }

    @Test
    @DisplayName("Should return 12 months for current and previous year with zero-filled totals")
    void testGetMonthlyTotalsForYearWithPreviousYearComparisonAndZeroFill() {
        Expense januaryCurrent = new Expense();
        januaryCurrent.setDueDate(LocalDate.of(2026, 1, 5));
        januaryCurrent.setAmount(BigDecimal.valueOf(50));

        Expense marchCurrent = new Expense();
        marchCurrent.setDueDate(LocalDate.of(2026, 3, 10));
        marchCurrent.setAmount(BigDecimal.valueOf(70));

        Expense februaryPrevious = new Expense();
        februaryPrevious.setDueDate(LocalDate.of(2025, 2, 20));
        februaryPrevious.setAmount(BigDecimal.valueOf(30));

        when(expenseRepository.findByTenantIdAndStatusAndDueDateBetweenAndDeletedFalse(
                tenantId, ExpenseStatus.PAID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(januaryCurrent, marchCurrent));
        when(expenseRepository.findByTenantIdAndStatusAndDueDateBetweenAndDeletedFalse(
                tenantId, ExpenseStatus.PAID, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)))
                .thenReturn(List.of(februaryPrevious));

        ExpenseMonthlyChartResponse response = expenseService.getMonthlyTotalsForYear(2026);

        assertEquals(12, response.totals().size());
        assertEquals(12, response.previousYearTotals().size());

        assertEquals(BigDecimal.valueOf(50), response.totals().get(0).total());
        assertEquals(BigDecimal.ZERO, response.totals().get(1).total());
        assertEquals(BigDecimal.valueOf(70), response.totals().get(2).total());

        assertEquals(BigDecimal.ZERO, response.previousYearTotals().get(0).total());
        assertEquals(BigDecimal.valueOf(30), response.previousYearTotals().get(1).total());
        assertEquals(BigDecimal.ZERO, response.previousYearTotals().get(2).total());
    }

    @Test
    @DisplayName("Should default missing year and month to current date for daily chart totals")
    void testGetDailyTotalsForMonthDefaultsToCurrentWhenMissing() {
        LocalDate today = LocalDate.now();
        LocalDate expectedStart = today.withDayOfMonth(1);
        LocalDate expectedEnd = today.withDayOfMonth(today.lengthOfMonth());

        when(expenseRepository.findByTenantIdAndStatusAndDueDateBetweenAndDeletedFalse(
                tenantId, ExpenseStatus.PAID, expectedStart, expectedEnd))
                .thenReturn(List.of());

        ExpenseDailyChartResponse response = expenseService.getDailyTotalsForMonth(null, null);

        assertEquals(today.getYear(), response.year());
        assertEquals(today.getMonthValue(), response.month());
        assertEquals(today.lengthOfMonth(), response.totals().size());
    }

    @Test
    @DisplayName("Should default missing year to current year for monthly chart totals")
    void testGetMonthlyTotalsForYearDefaultsToCurrentWhenMissing() {
        int currentYear = LocalDate.now().getYear();

        when(expenseRepository.findByTenantIdAndStatusAndDueDateBetweenAndDeletedFalse(
                tenantId, ExpenseStatus.PAID, LocalDate.of(currentYear, 1, 1), LocalDate.of(currentYear, 12, 31)))
                .thenReturn(List.of());
        when(expenseRepository.findByTenantIdAndStatusAndDueDateBetweenAndDeletedFalse(
                tenantId, ExpenseStatus.PAID, LocalDate.of(currentYear - 1, 1, 1), LocalDate.of(currentYear - 1, 12, 31)))
                .thenReturn(List.of());

        ExpenseMonthlyChartResponse response = expenseService.getMonthlyTotalsForYear(null);

        assertEquals(currentYear, response.year());
        assertEquals(12, response.totals().size());
        assertEquals(12, response.previousYearTotals().size());
    }
}

