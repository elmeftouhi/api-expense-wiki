package com.elmeftouhi.expense.budget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elmeftouhi.expense.activity.service.ActivityLogService;
import com.elmeftouhi.expense.budget.dto.BudgetCreateRequest;
import com.elmeftouhi.expense.budget.dto.BudgetPeriodResponse;
import com.elmeftouhi.expense.budget.dto.BudgetResponse;
import com.elmeftouhi.expense.budget.entity.Budget;
import com.elmeftouhi.expense.budget.entity.BudgetPeriodInstance;
import com.elmeftouhi.expense.budget.repository.BudgetPeriodInstanceRepository;
import com.elmeftouhi.expense.budget.repository.BudgetRepository;
import com.elmeftouhi.expense.budget.service.BudgetService;
import com.elmeftouhi.expense.budget.service.BudgetTrackingService;
import com.elmeftouhi.expense.category.entity.Category;
import com.elmeftouhi.expense.category.repository.CategoryRepository;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.security.TenantPermissionService;
import com.elmeftouhi.expense.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTests {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private BudgetPeriodInstanceRepository budgetPeriodInstanceRepository;

    @Mock
    private BudgetTrackingService budgetTrackingService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private BudgetService budgetService;

    @Mock
    private TenantPermissionService tenantPermissionService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getHistory_rejectsInvalidMonthsBackRange() {
        Long tenantId = 1L;
        TenantContext.setTenantId(tenantId);

        Budget budget = new Budget();
        budget.setId(10L);
        budget.assignTenantId(tenantId);
        when(budgetRepository.findByIdAndTenantId(10L, tenantId)).thenReturn(Optional.of(budget));

        assertThrows(BadRequestException.class, () -> budgetService.getHistory(10L, 0));
        assertThrows(BadRequestException.class, () -> budgetService.getHistory(10L, 61));
    }

    @Test
    void getHistory_returnsPeriodsFromTrackingService() {
        Long tenantId = 1L;
        TenantContext.setTenantId(tenantId);

        Budget budget = new Budget();
        budget.setId(10L);
        budget.assignTenantId(tenantId);

        BudgetPeriodInstance periodInstance = new BudgetPeriodInstance();
        periodInstance.setId(100L);
        periodInstance.setStartDate(LocalDate.of(2026, 10, 1));
        periodInstance.setEndDate(LocalDate.of(2026, 10, 31));
        periodInstance.setAllocatedAmount(BigDecimal.valueOf(500));
        periodInstance.setSpentAmount(BigDecimal.ZERO);
        periodInstance.setStatus(BudgetPeriodStatus.OPEN);
        periodInstance.setWarningType(BudgetWarningType.PERCENTAGE);
        periodInstance.setWarningThreshold(BigDecimal.valueOf(80));

        when(budgetRepository.findByIdAndTenantId(10L, tenantId)).thenReturn(Optional.of(budget));
        when(budgetTrackingService.ensureHistory(budget, tenantId, 6)).thenReturn(List.of(periodInstance));

        List<BudgetPeriodResponse> response = budgetService.getHistory(10L, 6);

        assertEquals(1, response.size());
        assertEquals(100L, response.get(0).id());
    }

    @Test
    void create_recalculatesCurrentPeriodAfterCreation() {
        Long tenantId = 1L;
        TenantContext.setTenantId(tenantId);

        Category category = new Category();
        category.setId(5L);
        category.setName("Food");

        BudgetCreateRequest request = new BudgetCreateRequest(
                "Main",
                "desc",
                BudgetPeriodicity.MONTHLY,
                BigDecimal.valueOf(500),
                BudgetWarningType.PERCENTAGE,
                BigDecimal.valueOf(80),
                Set.of(5L),
                true
        );

        Budget saved = new Budget();
        saved.setId(20L);
        saved.assignTenantId(tenantId);
        saved.setName("Main");
        saved.setDescription("desc");
        saved.setPeriodicity(BudgetPeriodicity.MONTHLY);
        saved.setAllocatedAmount(BigDecimal.valueOf(500));
        saved.setWarningType(BudgetWarningType.PERCENTAGE);
        saved.setWarningThreshold(BigDecimal.valueOf(80));
        saved.setCategories(Set.of(category));
        saved.setActive(true);

        BudgetPeriodInstance current = new BudgetPeriodInstance();
        current.setId(200L);
        current.setStartDate(LocalDate.now().withDayOfMonth(1));
        current.setEndDate(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));
        current.setAllocatedAmount(BigDecimal.valueOf(500));
        current.setSpentAmount(BigDecimal.ZERO);
        current.setStatus(BudgetPeriodStatus.OPEN);
        current.setWarningType(BudgetWarningType.PERCENTAGE);
        current.setWarningThreshold(BigDecimal.valueOf(80));

        when(categoryRepository.findByTenantIdAndIdIn(tenantId, Set.of(5L))).thenReturn(List.of(category));
        when(budgetRepository.save(any(Budget.class))).thenReturn(saved);
        when(budgetPeriodInstanceRepository.findByBudgetIdAndTenantIdAndStartDate(eq(20L), eq(tenantId), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(budgetPeriodInstanceRepository.save(any(BudgetPeriodInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(budgetPeriodInstanceRepository.findCurrentByBudgetId(eq(20L), eq(tenantId), any(LocalDate.class)))
                .thenReturn(Optional.of(current));

        BudgetResponse response = budgetService.create(request);

        assertEquals(20L, response.id());
        verify(budgetTrackingService).recalculateForReferenceDate(eq(saved), eq(tenantId), any(LocalDate.class), eq(true));
    }
}

