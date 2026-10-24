package com.elmeftouhi.expense.expense;

import com.elmeftouhi.expense.expense.entity.Expense;
import com.elmeftouhi.expense.expense.repository.ExpenseRepository;
import com.elmeftouhi.expense.expense.service.ExpenseService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elmeftouhi.expense.activity.service.ActivityLogService;
import com.elmeftouhi.expense.budget.service.BudgetTrackingService;
import com.elmeftouhi.expense.category.entity.Category;
import com.elmeftouhi.expense.category.repository.CategoryRepository;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.paymentmethod.service.PaymentMethodService;
import com.elmeftouhi.expense.expense.dto.ExpenseResponse;
import com.elmeftouhi.expense.shared.exception.BadRequestException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ExpenseServicePagingTests {

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

    private final Long tenantId = 1L;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(tenantId);
    }

    @Test
    void findAll_defaultsToCurrentYearWhenNoYearOrMonthProvided() {
        Category category = new Category();
        category.setId(7L);
        category.setName("Food");

        Expense expense = new Expense();
        expense.setId(10L);
        expense.setCategory(category);
        expense.setDescription("Lunch");
        expense.setAmount(BigDecimal.TEN);
        expense.setCurrency("USD");
        expense.setType(ExpenseType.NORMAL);
        expense.setStatus(ExpenseStatus.PENDING);
        expense.setDueDate(LocalDate.now());

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "dueDate"));
        Page<Expense> page = new PageImpl<>(List.of(expense), pageable, 1);

        int currentYear = LocalDate.now().getYear();
        LocalDate start = LocalDate.of(currentYear, 1, 1);
        LocalDate end = LocalDate.of(currentYear, 12, 31);

        when(expenseRepository.findPageByTenantAndFilters(eq(tenantId), eq(start), eq(end), isNull(), isNull(), eq(pageable)))
                .thenReturn(page);

        Page<ExpenseResponse> result = expenseService.findAll(null, null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(expenseRepository).findPageByTenantAndFilters(eq(tenantId), eq(start), eq(end), isNull(), isNull(), eq(pageable));
    }

    @Test
    void findAll_filtersByYearMonthAndCategory() {
        Long categoryId = 5L;
        int year = 2026;
        int month = 9;

        Category category = new Category();
        category.setId(categoryId);
        category.setName("Utilities");

        Expense expense = new Expense();
        expense.setId(11L);
        expense.setCategory(category);
        expense.setDescription("Internet");
        expense.setAmount(BigDecimal.valueOf(45));
        expense.setCurrency("USD");
        expense.setType(ExpenseType.NORMAL);
        expense.setStatus(ExpenseStatus.PENDING);
        expense.setDueDate(LocalDate.of(2026, 9, 15));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Expense> page = new PageImpl<>(List.of(expense), pageable, 1);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(java.util.Optional.of(category));
        when(expenseRepository.findPageByTenantAndFilters(tenantId, start, end, categoryId, null, pageable)).thenReturn(page);

        Page<ExpenseResponse> result = expenseService.findAll(year, month, categoryId, null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(categoryRepository).findByIdAndTenantId(categoryId, tenantId);
        verify(expenseRepository).findPageByTenantAndFilters(tenantId, start, end, categoryId, null, pageable);
    }

    @Test
    void findAll_filtersBySubscriptionType() {
        Expense expense = new Expense();
        expense.setId(12L);
        expense.setDescription("Netflix");
        expense.setAmount(BigDecimal.valueOf(20));
        expense.setCurrency("USD");
        expense.setType(ExpenseType.SUBSCRIPTION);
        expense.setStatus(ExpenseStatus.PENDING);
        expense.setDueDate(LocalDate.of(2026, 9, 20));

        Category category = new Category();
        category.setId(8L);
        category.setName("Entertainment");
        expense.setCategory(category);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Expense> page = new PageImpl<>(List.of(expense), pageable, 1);

        int currentYear = LocalDate.now().getYear();
        LocalDate start = LocalDate.of(currentYear, 1, 1);
        LocalDate end = LocalDate.of(currentYear, 12, 31);

        when(expenseRepository.findPageByTenantAndFilters(eq(tenantId), eq(start), eq(end), isNull(), eq(ExpenseType.SUBSCRIPTION), eq(pageable)))
                .thenReturn(page);

        Page<ExpenseResponse> result = expenseService.findAll(null, null, null, ExpenseType.SUBSCRIPTION, pageable);

        assertEquals(1, result.getTotalElements());
        verify(expenseRepository).findPageByTenantAndFilters(eq(tenantId), eq(start), eq(end), isNull(), eq(ExpenseType.SUBSCRIPTION), eq(pageable));
    }

    @Test
    void findAll_rejectsInvalidMonth() {
        Pageable pageable = PageRequest.of(0, 20);
        assertThrows(BadRequestException.class, () -> expenseService.findAll(2026, 13, null, null, pageable));
    }

    @Test
    void findAll_rejectsMonthWithoutYear() {
        Pageable pageable = PageRequest.of(0, 20);
        assertThrows(BadRequestException.class, () -> expenseService.findAll(null, 9, null, null, pageable));
    }
}

