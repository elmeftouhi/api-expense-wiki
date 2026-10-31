package com.elmeftouhi.expense.budget.service;

import com.elmeftouhi.expense.budget.BudgetPeriodStatus;
import com.elmeftouhi.expense.budget.BudgetPeriodicity;
import com.elmeftouhi.expense.budget.BudgetWarningType;
import com.elmeftouhi.expense.budget.entity.Budget;
import com.elmeftouhi.expense.budget.entity.BudgetPeriodInstance;
import com.elmeftouhi.expense.budget.repository.BudgetRepository;
import com.elmeftouhi.expense.budget.repository.BudgetPeriodInstanceRepository;
import com.elmeftouhi.expense.activity.ActivityAction;
import com.elmeftouhi.expense.activity.service.ActivityLogService;
import com.elmeftouhi.expense.activity.ActivitySubjectType;
import com.elmeftouhi.expense.budget.dto.BudgetCreateRequest;
import com.elmeftouhi.expense.budget.dto.BudgetPeriodResponse;
import com.elmeftouhi.expense.budget.dto.BudgetPeriodUpdateRequest;
import com.elmeftouhi.expense.budget.dto.BudgetResponse;
import com.elmeftouhi.expense.category.entity.Category;
import com.elmeftouhi.expense.category.repository.CategoryRepository;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.security.TenantPermissionService;
import com.elmeftouhi.expense.shared.exception.BadRequestException;
import com.elmeftouhi.expense.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetPeriodInstanceRepository budgetPeriodInstanceRepository;
    private final BudgetTrackingService budgetTrackingService;
    private final CategoryRepository categoryRepository;
    private final ActivityLogService activityLogService;
    private final TenantPermissionService tenantPermissionService;

    @Transactional(readOnly = true)
    public List<BudgetResponse> findAll() {
        Long tenantId = TenantContext.getRequiredTenantId();
        LocalDate today = LocalDate.now();
        return budgetRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(budget -> toResponse(budget, resolveCurrentPeriod(budget.getId(), tenantId, today)))
                .toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponse getById(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Budget budget = loadBudget(id, tenantId);
        return toResponse(budget, resolveCurrentPeriod(budget.getId(), tenantId, LocalDate.now()));
    }

    @Transactional
    public BudgetResponse create(BudgetCreateRequest request) {
        tenantPermissionService.requireEditor();
        Long tenantId = TenantContext.getRequiredTenantId();
        validateWarningThreshold(request.warningType(), request.warningThreshold());

        Budget budget = new Budget();
        budget.assignTenantId(tenantId);
        applyRequest(budget, request, tenantId);

        Budget saved = budgetRepository.save(budget);
        createInitialPeriodInstance(saved, tenantId);
        budgetTrackingService.recalculateForReferenceDate(saved, tenantId, LocalDate.now(), true);

        BudgetResponse response = toResponse(saved, resolveCurrentPeriod(saved.getId(), tenantId, LocalDate.now()));
        logActivity(ActivityAction.CREATE, response, "Budget created");
        return response;
    }

    @Transactional
    public BudgetResponse update(Long id, BudgetCreateRequest request) {
        tenantPermissionService.requireEditor();
        Long tenantId = TenantContext.getRequiredTenantId();
        validateWarningThreshold(request.warningType(), request.warningThreshold());

        Budget budget = loadBudget(id, tenantId);
        applyRequest(budget, request, tenantId);
        budget.setUpdatedAt(Instant.now());

        Budget saved = budgetRepository.save(budget);
        budgetTrackingService.recalculateForReferenceDate(saved, tenantId, LocalDate.now(), true);
        BudgetResponse response = toResponse(saved, resolveCurrentPeriod(saved.getId(), tenantId, LocalDate.now()));
        logActivity(ActivityAction.UPDATE, response, "Budget updated");
        return response;
    }

    @Transactional(readOnly = true)
    public List<BudgetPeriodResponse> getPeriods(Long budgetId) {
        Long tenantId = TenantContext.getRequiredTenantId();
        loadBudget(budgetId, tenantId);
        return budgetPeriodInstanceRepository.findByBudgetIdAndTenantIdOrderByStartDateDesc(budgetId, tenantId)
                .stream()
                .map(this::toPeriodResponse)
                .toList();
    }

    @Transactional
    public List<BudgetPeriodResponse> getHistory(Long budgetId, Integer monthsBack) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Budget budget = loadBudget(budgetId, tenantId);
        int effectiveMonthsBack = monthsBack == null ? 12 : monthsBack;
        if (effectiveMonthsBack < 1 || effectiveMonthsBack > 60) {
            throw new BadRequestException("monthsBack must be between 1 and 60");
        }

        return budgetTrackingService.ensureHistory(budget, tenantId, effectiveMonthsBack)
                .stream()
                .map(this::toPeriodResponse)
                .toList();
    }

    @Transactional
    public BudgetPeriodResponse updatePeriod(Long budgetId, Long periodId, BudgetPeriodUpdateRequest request) {
        tenantPermissionService.requireEditor();
        Long tenantId = TenantContext.getRequiredTenantId();
        validateWarningThreshold(request.warningType(), request.warningThreshold());
        loadBudget(budgetId, tenantId);

        BudgetPeriodInstance periodInstance = budgetPeriodInstanceRepository.findByIdAndTenantId(periodId, tenantId)
                .filter(period -> period.getBudget().getId().equals(budgetId))
                .orElseThrow(() -> new ResourceNotFoundException("Budget period not found"));

        periodInstance.setAllocatedAmount(request.allocatedAmount());
        periodInstance.setWarningType(request.warningType());
        periodInstance.setWarningThreshold(request.warningThreshold());
        periodInstance.setStatus(periodInstance.getSpentAmount().compareTo(request.allocatedAmount()) >= 0
                ? BudgetPeriodStatus.CLOSED
                : BudgetPeriodStatus.OPEN);
        periodInstance.setUpdatedAt(Instant.now());

        BudgetPeriodInstance saved = budgetPeriodInstanceRepository.save(periodInstance);

        BudgetResponse budgetResponse = toResponse(saved.getBudget(), toPeriodResponse(saved));
        logActivity(ActivityAction.UPDATE, budgetResponse, "Budget period updated");

        return toPeriodResponse(saved);
    }

    private void createInitialPeriodInstance(Budget budget, Long tenantId) {
        LocalDate today = LocalDate.now();
        DateRange range = computeRange(today, budget.getPeriodicity());

        boolean exists = budgetPeriodInstanceRepository.findByBudgetIdAndTenantIdAndStartDate(
                budget.getId(),
                tenantId,
                range.startDate()
        ).isPresent();

        if (exists) {
            return;
        }

        BudgetPeriodInstance periodInstance = new BudgetPeriodInstance();
        periodInstance.assignTenantId(tenantId);
        periodInstance.setBudget(budget);
        periodInstance.setStartDate(range.startDate());
        periodInstance.setEndDate(range.endDate());
        periodInstance.setAllocatedAmount(budget.getAllocatedAmount());
        periodInstance.setSpentAmount(BigDecimal.ZERO);
        periodInstance.setStatus(BudgetPeriodStatus.OPEN);
        periodInstance.setWarningType(budget.getWarningType());
        periodInstance.setWarningThreshold(budget.getWarningThreshold());
        budgetPeriodInstanceRepository.save(periodInstance);
    }

    private DateRange computeRange(LocalDate date, BudgetPeriodicity periodicity) {
        return switch (periodicity) {
            case MONTHLY -> {
                LocalDate start = date.withDayOfMonth(1);
                yield new DateRange(start, start.with(TemporalAdjusters.lastDayOfMonth()));
            }
            case QUARTERLY -> {
                int quarterStartMonth = ((date.getMonthValue() - 1) / 3) * 3 + 1;
                LocalDate start = LocalDate.of(date.getYear(), quarterStartMonth, 1);
                LocalDate end = start.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth());
                yield new DateRange(start, end);
            }
            case ANNUAL -> {
                LocalDate start = LocalDate.of(date.getYear(), 1, 1);
                yield new DateRange(start, LocalDate.of(date.getYear(), 12, 31));
            }
        };
    }

    private Budget loadBudget(Long id, Long tenantId) {
        return budgetRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
    }

    private BudgetPeriodResponse resolveCurrentPeriod(Long budgetId, Long tenantId, LocalDate date) {
        return budgetPeriodInstanceRepository.findCurrentByBudgetId(budgetId, tenantId, date)
                .map(this::toPeriodResponse)
                .orElse(null);
    }

    private void applyRequest(Budget budget, BudgetCreateRequest request, Long tenantId) {
        budget.setName(normalizeName(request.name()));
        budget.setDescription(normalizeOptional(request.description()));
        budget.setPeriodicity(request.periodicity());
        budget.setAllocatedAmount(request.allocatedAmount());
        budget.setWarningType(request.warningType());
        budget.setWarningThreshold(request.warningThreshold());
        budget.setActive(Boolean.TRUE.equals(request.active()));
        budget.setCategories(resolveCategories(request.categoryIds(), tenantId));
    }

    private Set<Category> resolveCategories(Set<Long> categoryIds, Long tenantId) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new BadRequestException("At least one category is required");
        }

        Set<Long> distinctIds = new LinkedHashSet<>(categoryIds);
        List<Category> categories = categoryRepository.findByTenantIdAndIdIn(tenantId, distinctIds);

        if (categories.size() != distinctIds.size()) {
            throw new ResourceNotFoundException("One or more categories were not found");
        }

        return new LinkedHashSet<>(categories);
    }

    private void validateWarningThreshold(BudgetWarningType warningType, BigDecimal warningThreshold) {
        if (warningType == BudgetWarningType.PERCENTAGE && warningThreshold.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("warningThreshold must be <= 100 when warningType is PERCENTAGE");
        }
    }

    private BudgetResponse toResponse(Budget budget, BudgetPeriodResponse currentPeriod) {
        Set<Long> categoryIds = budget.getCategories().stream().map(Category::getId).collect(java.util.stream.Collectors.toSet());

        return new BudgetResponse(
                budget.getId(),
                budget.getName(),
                budget.getDescription(),
                budget.getPeriodicity(),
                budget.getAllocatedAmount(),
                budget.getWarningType(),
                budget.getWarningThreshold(),
                budget.isActive(),
                categoryIds,
                budget.getTenantId(),
                currentPeriod,
                budget.getCreatedAt(),
                budget.getUpdatedAt()
        );
    }

    private BudgetPeriodResponse toPeriodResponse(BudgetPeriodInstance periodInstance) {
        return new BudgetPeriodResponse(
                periodInstance.getId(),
                periodInstance.getStartDate(),
                periodInstance.getEndDate(),
                periodInstance.getAllocatedAmount(),
                periodInstance.getSpentAmount(),
                periodInstance.getStatus(),
                periodInstance.getWarningType(),
                periodInstance.getWarningThreshold(),
                periodInstance.isWarningSent(),
                periodInstance.isExceededSent(),
                periodInstance.getCreatedAt(),
                periodInstance.getUpdatedAt()
        );
    }

    private String normalizeName(String name) {
        String trimmed = name == null ? null : name.trim();
        if (trimmed == null || trimmed.isBlank()) {
            throw new BadRequestException("Budget name is required");
        }
        return trimmed;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void logActivity(ActivityAction action, BudgetResponse budget, String message) {
        activityLogService.record(
                action,
                ActivitySubjectType.BUDGET,
                budget.id(),
                budget.name(),
                message,
                buildActivityMetadata(budget)
        );
    }

    private String buildActivityMetadata(BudgetResponse budget) {
        return "{" +
                "\"id\":" + budget.id() + ',' +
                "\"name\":\"" + escapeJson(budget.name()) + "\"," +
                "\"periodicity\":\"" + budget.periodicity() + "\"," +
                "\"allocatedAmount\":" + budget.allocatedAmount() +
                "}";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}

