package com.elmeftouhi.expense.expense;

import com.elmeftouhi.expense.activity.ActivityAction;
import com.elmeftouhi.expense.activity.ActivityLogService;
import com.elmeftouhi.expense.activity.ActivitySubjectType;
import com.elmeftouhi.expense.category.Category;
import com.elmeftouhi.expense.category.CategoryRepository;
import com.elmeftouhi.expense.expense.dto.ExpenseCreateRequest;
import com.elmeftouhi.expense.expense.dto.ExpenseResponse;
import com.elmeftouhi.expense.expense.dto.ExpenseStatisticsResponse;
import com.elmeftouhi.expense.expense.dto.SubscriptionCreateRequest;
import com.elmeftouhi.expense.expense.dto.SubscriptionResponse;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.security.JwtPrincipal;
import com.elmeftouhi.expense.shared.exception.BadRequestException;
import com.elmeftouhi.expense.shared.exception.ResourceNotFoundException;
import com.elmeftouhi.expense.tenantconfig.ConfigurationContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private final ExpenseSubscriptionRepository subscriptionRepository;
    private final SubscriptionOccurrenceRepository occurrenceRepository;
    private final CategoryRepository categoryRepository;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> findAll(Integer year, Integer month, Long categoryId, Pageable pageable) {
        Long tenantId = TenantContext.getRequiredTenantId();

        if (month != null && (month < 1 || month > 12)) {
            throw new BadRequestException("month must be between 1 and 12");
        }
        if (month != null && year == null) {
            throw new BadRequestException("year is required when month is provided");
        }

        if (categoryId != null) {
            categoryRepository.findByIdAndTenantId(categoryId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

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

        return expenseRepository.findPageByTenantAndFilters(tenantId, startDate, endDate, categoryId, pageable)
                .map(this::toResponse);
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

        Expense expense = new Expense();
        expense.assignTenantId(tenantId);
        expense.setCategory(category);
        expense.setUserId(userId);
        expense.setDescription(request.description());
        expense.setAmount(request.amount());
        expense.setCurrency(ConfigurationContext.getCurrency().name());
        expense.setType(request.type() != null ? request.type() : ExpenseType.NORMAL);
        expense.setStatus(request.paidDate() != null ? ExpenseStatus.PAID : ExpenseStatus.PENDING);
        expense.setDueDate(request.dueDate());
        expense.setPaidDate(request.paidDate());
        expense.setPaymentMethod(request.paymentMethod());
        expense.setNotes(request.notes());

        Expense saved = expenseRepository.save(expense);

        ExpenseResponse response = toResponse(saved);
        logActivity(ActivityAction.CREATE, response, "Expense created");

        return response;
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseCreateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Expense expense = loadExpense(id, tenantId);

        Category category = categoryRepository.findByIdAndTenantId(request.categoryId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        expense.setCategory(category);
        expense.setDescription(request.description());
        expense.setAmount(request.amount());
        expense.setDueDate(request.dueDate());
        expense.setPaidDate(request.paidDate());
        expense.setStatus(request.paidDate() != null ? ExpenseStatus.PAID : ExpenseStatus.PENDING);
        expense.setPaymentMethod(request.paymentMethod());
        expense.setNotes(request.notes());
        expense.setUpdatedAt(Instant.now());

        Expense saved = expenseRepository.save(expense);

        ExpenseResponse response = toResponse(saved);
        logActivity(ActivityAction.UPDATE, response, "Expense updated");

        return response;
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Expense expense = loadExpense(id, tenantId);

        ExpenseResponse response = toResponse(expense);
        expense.markAsDeleted();
        expenseRepository.save(expense);

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

        expense.markAsPaid(paidDate);
        Expense saved = expenseRepository.save(expense);

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

        if (newStatus == ExpenseStatus.PAID && paidDate != null) {
            expense.markAsPaid(paidDate);
        } else {
            expense.setStatus(newStatus);
            expense.setUpdatedAt(Instant.now());
        }

        Expense saved = expenseRepository.save(expense);

        ExpenseResponse response = toResponse(saved);
        logActivity(ActivityAction.STATUS_CHANGED, response, "Expense status changed to " + newStatus);

        return response;
    }

    @Transactional
    public SubscriptionResponse createSubscription(Long expenseId, SubscriptionCreateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Expense expense = loadExpense(expenseId, tenantId);

        if (expense.getSubscription() != null) {
            throw new BadRequestException("This expense already has a subscription");
        }

        if (expense.getType() != ExpenseType.SUBSCRIPTION) {
            throw new BadRequestException("Only subscription-type expenses can have subscriptions");
        }

        ExpenseSubscription subscription = new ExpenseSubscription();
        subscription.assignTenantId(tenantId);
        subscription.setExpense(expense);
        subscription.setFrequency(request.frequency());
        subscription.setCustomIntervalDays(request.customIntervalDays());
        subscription.setNextOccurrenceDate(expense.getDueDate().plusDays(
                request.frequency() == SubscriptionFrequency.CUSTOM
                    ? request.customIntervalDays()
                    : request.frequency().getDays()
        ));
        subscription.setEndDate(request.endDate());
        subscription.setActive(true);

        ExpenseSubscription saved = subscriptionRepository.save(subscription);
        expense.setSubscription(saved);
        expenseRepository.save(expense);

        SubscriptionResponse response = toResponse(saved);
        logActivity(ActivityAction.CREATE, expense, "Subscription created");

        return response;
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(Long expenseId) {
        Long tenantId = TenantContext.getRequiredTenantId();
        loadExpense(expenseId, tenantId);

        ExpenseSubscription subscription = subscriptionRepository.findByExpenseId(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        return toResponse(subscription);
    }

    @Transactional
    public SubscriptionResponse updateSubscription(Long expenseId, SubscriptionCreateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        loadExpense(expenseId, tenantId);

        ExpenseSubscription subscription = subscriptionRepository.findByExpenseId(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        subscription.setFrequency(request.frequency());
        subscription.setCustomIntervalDays(request.customIntervalDays());
        subscription.setEndDate(request.endDate());
        subscription.setUpdatedAt(Instant.now());

        ExpenseSubscription saved = subscriptionRepository.save(subscription);

        SubscriptionResponse response = toResponse(saved);
        logActivity(ActivityAction.UPDATE, subscription.getExpense(), "Subscription updated");

        return response;
    }

    @Transactional
    public SubscriptionResponse toggleSubscriptionStatus(Long expenseId, Boolean active) {
        Long tenantId = TenantContext.getRequiredTenantId();
        loadExpense(expenseId, tenantId);

        ExpenseSubscription subscription = subscriptionRepository.findByExpenseId(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        subscription.setActive(active);
        subscription.setUpdatedAt(Instant.now());

        ExpenseSubscription saved = subscriptionRepository.save(subscription);

        SubscriptionResponse response = toResponse(saved);
        String action = active ? "Subscription activated" : "Subscription deactivated";
        logActivity(ActivityAction.STATUS_CHANGED, subscription.getExpense(), action);

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

        BigDecimal totalExpenses = expenseRepository.sumByTenantAndStatusAndDateRange(
                tenantId, ExpenseStatus.PENDING, startDate, endDate);
        BigDecimal totalPaid = expenseRepository.sumByTenantAndStatusAndDateRange(
                tenantId, ExpenseStatus.PAID, startDate, endDate);
        BigDecimal totalOverdue = expenseRepository.sumByTenantAndStatusAndDateRange(
                tenantId, ExpenseStatus.OVERDUE, startDate, endDate);

        List<Expense> pendingExpenses = expenseRepository
                .findByTenantIdAndStatusAndDeletedFalse(tenantId, ExpenseStatus.PENDING);
        List<Expense> overdueExpenses = expenseRepository
                .findByTenantIdAndStatusAndDeletedFalse(tenantId, ExpenseStatus.OVERDUE);

        List<ExpenseStatisticsResponse.CategorySummary> byCategory = buildCategorySummary(tenantId, startDate, endDate);

        return new ExpenseStatisticsResponse(
                totalExpenses,
                totalPaid,
                totalPaid,
                totalOverdue,
                overdueExpenses.size(),
                pendingExpenses.size(),
                byCategory
        );
    }

    private List<ExpenseStatisticsResponse.CategorySummary> buildCategorySummary(
            Long tenantId, LocalDate startDate, LocalDate endDate) {
        List<Category> categories = categoryRepository.findRootCategories(tenantId, false);
        List<ExpenseStatisticsResponse.CategorySummary> summaries = new ArrayList<>();

        for (Category category : categories) {
            BigDecimal total = expenseRepository.sumByCategoryAndDateRange(tenantId, category.getId(), startDate, endDate);
            BigDecimal budget = category.getMonthlyBudget() != null ? category.getMonthlyBudget() : BigDecimal.ZERO;
            int percentage = budget.compareTo(BigDecimal.ZERO) == 0 ? 0 :
                    total.multiply(BigDecimal.valueOf(100)).divide(budget, 0, RoundingMode.HALF_UP).intValue();

            summaries.add(new ExpenseStatisticsResponse.CategorySummary(
                    category.getId(),
                    category.getName(),
                    total,
                    budget,
                    Math.min(percentage, 100)
            ));
        }

        return summaries;
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
                expense.getPaymentMethod(),
                expense.getNotes(),
                expense.getSubscription() != null,
                expense.getTenantId(),
                expense.getCreatedAt(),
                expense.getUpdatedAt()
        );
    }

    private SubscriptionResponse toResponse(ExpenseSubscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getExpense().getId(),
                subscription.getFrequency(),
                subscription.getCustomIntervalDays(),
                subscription.getNextOccurrenceDate(),
                subscription.getEndDate(),
                subscription.getOccurrenceCount(),
                subscription.getActive(),
                subscription.getTenantId(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt()
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

