package com.elmeftouhi.expense.expense.service;

import com.elmeftouhi.expense.expense.entity.Expense;
import com.elmeftouhi.expense.expense.entity.Subscription;
import com.elmeftouhi.expense.expense.repository.ExpenseRepository;
import com.elmeftouhi.expense.expense.ExpenseStatus;
import com.elmeftouhi.expense.expense.ExpenseType;
import com.elmeftouhi.expense.activity.ActivityAction;
import com.elmeftouhi.expense.activity.service.ActivityLogService;
import com.elmeftouhi.expense.activity.ActivitySubjectType;
import com.elmeftouhi.expense.budget.service.BudgetTrackingService;
import com.elmeftouhi.expense.budget.entity.ExpenseBudgetSnapshot;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final ActivityLogService activityLogService;
    private final BudgetTrackingService budgetTrackingService;
    private final PaymentMethodService paymentMethodService;

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> findAll(
            Integer year,
            Integer month,
            List<Long> categoryIds,
            ExpenseType type,
            ExpenseStatus paymentStatus,
            String search,
            Pageable pageable) {
        return findAllWithAggregates(year, month, categoryIds, type, paymentStatus, search, pageable).page();
    }

    @Transactional(readOnly = true)
    public ExpensePageSearchResult findAllWithAggregates(
            Integer year,
            Integer month,
            List<Long> categoryIds,
            ExpenseType type,
            ExpenseStatus paymentStatus,
            String search,
            Pageable pageable) {
        Long tenantId = TenantContext.getRequiredTenantId();

        if (month != null && (month < 1 || month > 12)) {
            throw new BadRequestException("month must be between 1 and 12");
        }
        if (month != null && year == null) {
            throw new BadRequestException("year is required when month is provided");
        }
        if (paymentStatus != null && paymentStatus != ExpenseStatus.PAID && paymentStatus != ExpenseStatus.PENDING) {
            throw new BadRequestException("paymentStatus must be PAID or PENDING");
        }

        List<Long> effectiveCategoryIds = null;
        if (categoryIds != null && !categoryIds.isEmpty()) {
            effectiveCategoryIds = categoryIds.stream().distinct().toList();
            for (Long categoryId : effectiveCategoryIds) {
                categoryRepository.findByIdAndTenantId(categoryId, tenantId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            }
        }

        String normalizedSearch = (search != null && !search.isBlank())
                ? search.trim().toLowerCase(Locale.ROOT)
                : null;
        boolean applySearchFilter = normalizedSearch != null;
        String searchPattern = applySearchFilter ? "%" + normalizedSearch + "%" : "";

        LocalDate startDate;
        LocalDate endDate;

        if (year == null) {
            int currentYear = LocalDate.now().getYear();
            startDate = LocalDate.of(currentYear, 1, 1);
            endDate = LocalDate.of(currentYear, 12, 31);
        } else if (month == null) {
            startDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 12, 31);
        } else {
            YearMonth targetMonth = YearMonth.of(year, month);
            startDate = targetMonth.atDay(1);
            endDate = targetMonth.atEndOfMonth();
        }

        Page<ExpenseResponse> page = expenseRepository.findPageByTenantAndFilters(
                        tenantId,
                        startDate,
                        endDate,
                        effectiveCategoryIds,
                        type,
                        paymentStatus,
                        applySearchFilter,
                        searchPattern,
                        pageable)
                .map(this::toResponse);

        BigDecimal filteredTotalAmount = expenseRepository.sumByTenantAndFilters(
                tenantId,
                startDate,
                endDate,
                effectiveCategoryIds,
                type,
                paymentStatus,
                applySearchFilter,
                searchPattern);

        return new ExpensePageSearchResult(page, filteredTotalAmount != null ? filteredTotalAmount : BigDecimal.ZERO);
    }

    public record ExpensePageSearchResult(Page<ExpenseResponse> page, BigDecimal filteredTotalAmount) {
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getById(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Expense expense = loadExpense(id, tenantId);
        return toResponse(expense);
    }

    @Transactional
    public ExpenseResponse create(ExpenseCreateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Long userId = getCurrentUserId();

        Category category = categoryRepository.findByIdAndTenantId(request.categoryId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        PaymentMethod paymentMethod = paymentMethodService.resolveForUsage(tenantId, request.paymentMethodId());

        Expense expense = new Expense();
        expense.assignTenantId(tenantId);
        expense.setCategory(category);
        expense.setUserId(userId);
        expense.setDescription(request.description());
        expense.setAmount(request.amount());
        expense.setCurrency(ConfigurationContext.getCurrency().name());
        ExpenseType type = request.type() != null ? request.type() : ExpenseType.NORMAL;
        if (type == ExpenseType.SUBSCRIPTION) {
            throw new BadRequestException("Use /v1/subscriptions to create recurring subscriptions");
        }
        expense.setType(type);
        expense.setStatus(request.paidDate() != null ? ExpenseStatus.PAID : ExpenseStatus.PENDING);
        expense.setDueDate(request.dueDate());
        expense.setPaidDate(request.paidDate());
        expense.setPaymentMethod(paymentMethod);
        expense.setNotes(request.notes());
        Expense saved = expenseRepository.save(expense);
        budgetTrackingService.applyExpenseCreated(ExpenseBudgetSnapshot.fromExpense(saved));

        ExpenseResponse response = toResponse(saved);
        logActivity(ActivityAction.CREATE, response, "Expense created");

        return response;
    }

    @Transactional
    public Expense createGeneratedExpenseFromSubscription(Subscription subscription, LocalDate scheduledBillingDate) {
        return createGeneratedExpenseFromSubscription(subscription, scheduledBillingDate, null);
    }

    @Transactional
    public Expense createGeneratedExpenseFromSubscription(
            Subscription subscription,
            LocalDate scheduledBillingDate,
            LocalDate paidDate) {
        Expense expense = new Expense();
        expense.assignTenantId(subscription.getTenantId());
        expense.setCategory(subscription.getCategory());
        expense.setUserId(subscription.getOwnerUserId());
        expense.setDescription(subscription.getDescription() != null ? subscription.getDescription() : subscription.getName());
        expense.setAmount(subscription.getAmount());
        expense.setCurrency(subscription.getCurrency());
        expense.setType(ExpenseType.SUBSCRIPTION);
        expense.setStatus(paidDate != null ? ExpenseStatus.PAID : ExpenseStatus.PENDING);
        expense.setDueDate(scheduledBillingDate);
        expense.setPaidDate(paidDate);
        expense.setPaymentMethod(subscription.getPaymentMethod());
        expense.setNotes(subscription.getNotes());
        Expense saved = expenseRepository.save(expense);
        budgetTrackingService.applyExpenseCreated(ExpenseBudgetSnapshot.fromExpense(saved));
        logActivity(ActivityAction.CREATE, saved, "Subscription payment generated");
        return saved;
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseCreateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Expense expense = loadExpense(id, tenantId);
        ExpenseBudgetSnapshot previousSnapshot = ExpenseBudgetSnapshot.fromExpense(expense);

        Category category = categoryRepository.findByIdAndTenantId(request.categoryId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        PaymentMethod paymentMethod = paymentMethodService.resolveForUsage(tenantId, request.paymentMethodId());

        expense.setCategory(category);
        expense.setDescription(request.description());
        expense.setAmount(request.amount());
        expense.setDueDate(request.dueDate());
        expense.setPaidDate(request.paidDate());
        expense.setStatus(request.paidDate() != null ? ExpenseStatus.PAID : ExpenseStatus.PENDING);
        expense.setPaymentMethod(paymentMethod);
        expense.setNotes(request.notes());
        expense.setUpdatedAt(Instant.now());
        ExpenseType type = request.type() != null ? request.type() : ExpenseType.NORMAL;
        if (type == ExpenseType.SUBSCRIPTION && (expense.getType() == null || expense.getType() != ExpenseType.SUBSCRIPTION)) {
            throw new BadRequestException("Use /v1/subscriptions to define recurring subscriptions");
        }
        expense.setType(type);

        Expense saved = expenseRepository.save(expense);
        budgetTrackingService.applyExpenseUpdated(previousSnapshot, ExpenseBudgetSnapshot.fromExpense(saved));

        ExpenseResponse response = toResponse(saved);
        logActivity(ActivityAction.UPDATE, response, "Expense updated");

        return response;
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Expense expense = loadExpense(id, tenantId);
        ExpenseBudgetSnapshot snapshot = ExpenseBudgetSnapshot.fromExpense(expense);

        ExpenseResponse response = toResponse(expense);
        expense.markAsDeleted();
        expenseRepository.save(expense);
        budgetTrackingService.applyExpenseDeleted(snapshot);

        logActivity(ActivityAction.DELETE, response, "Expense deleted");
    }

    @Transactional
    public ExpenseResponse markAsPaid(Long id, LocalDate paidDate) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Expense expense = loadExpense(id, tenantId);

        if (paidDate == null) {
            throw new BadRequestException("paidDate is required when marking an expense as paid");
        }

        if (expense.getStatus() == ExpenseStatus.PAID) {
            throw new BadRequestException("Expense is already marked as paid");
        }

        ExpenseBudgetSnapshot previousSnapshot = ExpenseBudgetSnapshot.fromExpense(expense);

        expense.markAsPaid(paidDate);
        Expense saved = expenseRepository.save(expense);
        budgetTrackingService.applyExpenseUpdated(previousSnapshot, ExpenseBudgetSnapshot.fromExpense(saved));

        ExpenseResponse response = toResponse(saved);
        logActivity(ActivityAction.PAYMENT_ADDED, response, "Expense marked as paid");

        return response;
    }

    @Transactional
    public ExpenseResponse changeStatus(Long id, ExpenseStatus newStatus, LocalDate paidDate) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Expense expense = loadExpense(id, tenantId);

        if (newStatus == ExpenseStatus.PAID && paidDate == null) {
            throw new BadRequestException("paidDate is required when changing status to PAID");
        }

        ExpenseBudgetSnapshot previousSnapshot = ExpenseBudgetSnapshot.fromExpense(expense);

        if (newStatus == ExpenseStatus.PAID) {
            expense.markAsPaid(paidDate);
        } else {
            expense.setStatus(newStatus);
            expense.setUpdatedAt(Instant.now());
        }

        Expense saved = expenseRepository.save(expense);
        budgetTrackingService.applyExpenseUpdated(previousSnapshot, ExpenseBudgetSnapshot.fromExpense(saved));

        ExpenseResponse response = toResponse(saved);
        logActivity(ActivityAction.STATUS_CHANGED, response, "Expense status changed to " + newStatus);

        return response;
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> findOverdue() {
        Long tenantId = TenantContext.getRequiredTenantId();
        LocalDate today = LocalDate.now();
        List<Expense> overdue = expenseRepository.findByTenantIdAndStatusAndDueDateBeforeAndDeletedFalse(
                tenantId, ExpenseStatus.PENDING, today);
        return overdue.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ExpenseStatisticsResponse getStatistics(LocalDate startDate, LocalDate endDate) {
        Long tenantId = TenantContext.getRequiredTenantId();

        BigDecimal totalPending = expenseRepository.sumByTenantAndStatusAndDateRange(
                tenantId, ExpenseStatus.PENDING, startDate, endDate);
        BigDecimal totalPaid = expenseRepository.sumByTenantAndStatusAndDateRange(
                tenantId, ExpenseStatus.PAID, startDate, endDate);
        BigDecimal totalOverdue = expenseRepository.sumByTenantAndStatusAndDateRange(
                tenantId, ExpenseStatus.OVERDUE, startDate, endDate);
        BigDecimal totalExpenses = totalPending.add(totalPaid).add(totalOverdue);

        List<Expense> pendingExpenses = expenseRepository
                .findByTenantIdAndStatusAndDeletedFalse(tenantId, ExpenseStatus.PENDING);
        List<Expense> overdueExpenses = expenseRepository
                .findByTenantIdAndStatusAndDeletedFalse(tenantId, ExpenseStatus.OVERDUE);

        List<ExpenseStatisticsResponse.CategorySummary> byCategory = buildCategorySummary(tenantId, startDate, endDate);

        return new ExpenseStatisticsResponse(
                totalExpenses,
                totalPaid,
                totalPending,
                totalOverdue,
                overdueExpenses.size(),
                pendingExpenses.size(),
                byCategory
        );
    }

    @Transactional(readOnly = true)
    public ExpenseDailyChartResponse getDailyTotalsForMonth(Integer year, Integer month) {
        LocalDate today = LocalDate.now();
        int effectiveYear = year != null ? year : today.getYear();
        int effectiveMonth = month != null ? month : today.getMonthValue();

        if (effectiveMonth < 1 || effectiveMonth > 12) {
            throw new BadRequestException("month must be between 1 and 12");
        }

        Long tenantId = TenantContext.getRequiredTenantId();
        YearMonth yearMonth = YearMonth.of(effectiveYear, effectiveMonth);
        YearMonth previousYearMonth = yearMonth.minusMonths(1);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        LocalDate previousStartDate = previousYearMonth.atDay(1);
        LocalDate previousEndDate = previousYearMonth.atEndOfMonth();

        List<Expense> expenses = expenseRepository.findByTenantIdAndStatusAndDueDateBetweenAndDeletedFalse(
                tenantId,
                ExpenseStatus.PAID,
                startDate,
                endDate);
        List<Expense> previousMonthExpenses = expenseRepository.findByTenantIdAndStatusAndDueDateBetweenAndDeletedFalse(
                tenantId,
                ExpenseStatus.PAID,
                previousStartDate,
                previousEndDate);
        Map<Integer, BigDecimal> totalsByDay = aggregateTotalsByPeriod(expenses, true);
        Map<Integer, BigDecimal> previousTotalsByDay = aggregateTotalsByPeriod(previousMonthExpenses, true);

        List<ExpenseDailyChartResponse.DayTotal> totals = new ArrayList<>();
        List<ExpenseDailyChartResponse.DayTotal> previousMonthTotals = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            totals.add(new ExpenseDailyChartResponse.DayTotal(day, totalsByDay.getOrDefault(day, BigDecimal.ZERO)));
            BigDecimal previousMonthTotal = day <= previousYearMonth.lengthOfMonth()
                    ? previousTotalsByDay.getOrDefault(day, BigDecimal.ZERO)
                    : BigDecimal.ZERO;
            previousMonthTotals.add(new ExpenseDailyChartResponse.DayTotal(day, previousMonthTotal));
        }

        return new ExpenseDailyChartResponse(effectiveYear, effectiveMonth, totals, previousMonthTotals);
    }

    @Transactional(readOnly = true)
    public ExpenseMonthlyChartResponse getMonthlyTotalsForYear(Integer year) {
        int effectiveYear = year != null ? year : LocalDate.now().getYear();

        Long tenantId = TenantContext.getRequiredTenantId();
        List<ExpenseMonthlyChartResponse.MonthTotal> currentYearTotals = buildMonthlyTotalsForYear(tenantId, effectiveYear);
        List<ExpenseMonthlyChartResponse.MonthTotal> previousYearTotals = buildMonthlyTotalsForYear(tenantId, effectiveYear - 1);

        return new ExpenseMonthlyChartResponse(effectiveYear, currentYearTotals, previousYearTotals);
    }

    private List<ExpenseMonthlyChartResponse.MonthTotal> buildMonthlyTotalsForYear(Long tenantId, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        List<Expense> expenses = expenseRepository.findByTenantIdAndStatusAndDueDateBetweenAndDeletedFalse(
                tenantId,
                ExpenseStatus.PAID,
                startDate,
                endDate);
        Map<Integer, BigDecimal> totalsByMonth = aggregateTotalsByPeriod(expenses, false);

        List<ExpenseMonthlyChartResponse.MonthTotal> totals = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            totals.add(new ExpenseMonthlyChartResponse.MonthTotal(month, totalsByMonth.getOrDefault(month, BigDecimal.ZERO)));
        }

        return totals;
    }

    private Map<Integer, BigDecimal> aggregateTotalsByPeriod(List<Expense> expenses, boolean dayGranularity) {
        Map<Integer, BigDecimal> totals = new LinkedHashMap<>();
        for (Expense expense : expenses) {
            int period = dayGranularity ? expense.getDueDate().getDayOfMonth() : expense.getDueDate().getMonthValue();
            totals.merge(period, expense.getAmount(), BigDecimal::add);
        }
        return totals;
    }

    private List<ExpenseStatisticsResponse.CategorySummary> buildCategorySummary(
            Long tenantId, LocalDate startDate, LocalDate endDate) {
        List<Category> categories = categoryRepository.findRootCategories(tenantId, false);
        List<ExpenseStatisticsResponse.CategorySummary> summaries = new ArrayList<>();

        for (Category category : categories) {
            CategorySummaryNode node = buildCategorySummaryNode(tenantId, category, startDate, endDate);
            summaries.add(toCategorySummary(node, null));
        }

        return summaries;
    }

    private CategorySummaryNode buildCategorySummaryNode(
            Long tenantId,
            Category category,
            LocalDate startDate,
            LocalDate endDate) {
        List<Category> childCategories = categoryRepository.findChildCategories(tenantId, category.getId(), false);
        List<CategorySummaryNode> childNodes = new ArrayList<>();

        for (Category child : childCategories) {
            childNodes.add(buildCategorySummaryNode(tenantId, child, startDate, endDate));
        }

        BigDecimal total;
        if (childNodes.isEmpty()) {
            total = expenseRepository.sumByCategoryAndDateRange(tenantId, category.getId(), startDate, endDate);
        } else {
            total = childNodes.stream()
                    .map(CategorySummaryNode::total)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return new CategorySummaryNode(category.getId(), category.getName(), total, childNodes);
    }

    private ExpenseStatisticsResponse.CategorySummary toCategorySummary(
            CategorySummaryNode node,
            BigDecimal parentTotal) {
        List<ExpenseStatisticsResponse.CategorySummary> subCategories = node.subCategories().stream()
                .map(child -> toCategorySummary(child, node.total()))
                .toList();

        return new ExpenseStatisticsResponse.CategorySummary(
                node.categoryId(),
                node.categoryName(),
                node.total(),
                parentTotal,
                subCategories
        );
    }

    private record CategorySummaryNode(
            Long categoryId,
            String categoryName,
            BigDecimal total,
            List<CategorySummaryNode> subCategories
    ) {
    }

    private Expense loadExpense(Long id, Long tenantId) {
        return expenseRepository.findByIdAndTenantId(id, tenantId)
                .filter(e -> !e.getDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new BadRequestException("Authenticated user not found");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof JwtPrincipal jwtPrincipal) {
            return jwtPrincipal.userId();
        }

        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Authenticated user id is invalid");
        }
    }

    private ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getCategory().getId(),
                expense.getCategory().getName(),
                expense.getUserId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getCurrency(),
                expense.getType(),
                expense.getStatus(),
                expense.getDueDate(),
                expense.getPaidDate(),
                expense.getPaymentMethod() != null ? expense.getPaymentMethod().getId() : null,
                expense.getPaymentMethod() != null ? expense.getPaymentMethod().getName() : null,
                expense.getNotes(),
                expense.getType() == ExpenseType.SUBSCRIPTION,
                expense.getTenantId(),
                expense.getCreatedAt(),
                expense.getUpdatedAt()
        );
    }


    private void logActivity(ActivityAction action, ExpenseResponse expense, String message) {
        activityLogService.record(
                action,
                ActivitySubjectType.EXPENSE,
                expense.id(),
                expense.description(),
                message,
                buildActivityMetadata(expense)
        );
    }

    private void logActivity(ActivityAction action, Expense expense, String message) {
        activityLogService.record(
                action,
                ActivitySubjectType.EXPENSE,
                expense.getId(),
                expense.getDescription(),
                message,
                buildActivityMetadata(expense)
        );
    }

    private String buildActivityMetadata(ExpenseResponse expense) {
        return "{" +
                "\"id\":" + expense.id() + "," +
                "\"categoryId\":" + expense.categoryId() + "," +
                "\"amount\":" + expense.amount() + "," +
                "\"status\":\"" + expense.status() + "\"," +
                "\"dueDate\":\"" + expense.dueDate() + "\"" +
                "}";
    }

    private String buildActivityMetadata(Expense expense) {
        return "{" +
                "\"id\":" + expense.getId() + "," +
                "\"categoryId\":" + expense.getCategory().getId() + "," +
                "\"amount\":" + expense.getAmount() + "," +
                "\"status\":\"" + expense.getStatus() + "\"," +
                "\"dueDate\":\"" + expense.getDueDate() + "\"" +
                "}";
    }
}

