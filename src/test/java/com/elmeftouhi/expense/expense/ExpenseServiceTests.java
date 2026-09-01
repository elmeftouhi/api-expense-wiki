package com.elmeftouhi.expense.expense;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.elmeftouhi.expense.activity.ActivityLogService;
import com.elmeftouhi.expense.category.Category;
import com.elmeftouhi.expense.category.CategoryRepository;
import com.elmeftouhi.expense.expense.dto.ExpenseCreateRequest;
import com.elmeftouhi.expense.expense.dto.ExpenseResponse;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.security.JwtPrincipal;
import com.elmeftouhi.expense.shared.exception.BadRequestException;
import com.elmeftouhi.expense.shared.exception.ResourceNotFoundException;
import com.elmeftouhi.expense.tenantconfig.ConfigurationContext;
import com.elmeftouhi.expense.tenantconfig.Currency;
import com.elmeftouhi.expense.tenantconfig.TenantConfiguration;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    private ExpenseSubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionOccurrenceRepository occurrenceRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ActivityLogService activityLogService;

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
                "CARD",
                "Test notes"
        );

        Category category = new Category();
        category.setId(categoryId);
        category.setName("Food");

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
                "CARD",
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
                ExpenseType.SUBSCRIPTION,
                "CARD",
                "Test notes"
        );

        Category category = new Category();
        category.setId(categoryId);
        category.setName("Food");

        Expense savedExpense = new Expense();
        savedExpense.setId(2L);
        savedExpense.setCategory(category);
        savedExpense.setDescription(request.description());
        savedExpense.setAmount(request.amount());
        savedExpense.setCurrency("USD");
        savedExpense.setType(ExpenseType.SUBSCRIPTION);
        savedExpense.setStatus(ExpenseStatus.PENDING);
        savedExpense.setDueDate(request.dueDate());
        savedExpense.setPaidDate(null);

        when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(java.util.Optional.of(category));
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

        // When
        ExpenseResponse response = expenseService.create(request);

        // Then
        assertNotNull(response);
        assertEquals(ExpenseStatus.PENDING, response.status());
        assertNull(response.paidDate());
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
}

