package com.elmeftouhi.expense.budget.service;

import com.elmeftouhi.expense.budget.BudgetPeriodStatus;
import com.elmeftouhi.expense.budget.entity.ExpenseBudgetSnapshot;
import com.elmeftouhi.expense.budget.BudgetPeriodicity;
import com.elmeftouhi.expense.budget.BudgetWarningType;
import com.elmeftouhi.expense.budget.entity.Budget;
import com.elmeftouhi.expense.budget.entity.BudgetPeriodInstance;
import com.elmeftouhi.expense.budget.repository.BudgetRepository;
import com.elmeftouhi.expense.budget.repository.BudgetPeriodInstanceRepository;
import com.elmeftouhi.expense.category.entity.Category;
import com.elmeftouhi.expense.expense.ExpenseStatus;
import com.elmeftouhi.expense.expense.repository.ExpenseRepository;
import com.elmeftouhi.expense.notification.service.NotificationService;
import com.elmeftouhi.expense.notification.NotificationType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BudgetTrackingService {

    private final BudgetRepository budgetRepository;
    private final BudgetPeriodInstanceRepository budgetPeriodInstanceRepository;
    private final ExpenseRepository expenseRepository;
    private final NotificationService notificationService;

    @Transactional
    public void applyExpenseCreated(ExpenseBudgetSnapshot snapshot) {
        applySnapshot(snapshot, true, true);
    }

    @Transactional
    public void applyExpenseDeleted(ExpenseBudgetSnapshot snapshot) {
        applySnapshot(snapshot, false, false);
    }

    @Transactional
    public void applyExpenseUpdated(ExpenseBudgetSnapshot previousSnapshot, ExpenseBudgetSnapshot currentSnapshot) {
        applySnapshot(previousSnapshot, false, false);
        applySnapshot(currentSnapshot, true, false);
    }

    private void applySnapshot(ExpenseBudgetSnapshot snapshot, boolean increase, boolean allowCreatePeriodInstance) {
        if (snapshot == null
                || snapshot.categoryId() == null
                || snapshot.amount() == null
                || snapshot.amount().signum() <= 0
                || snapshot.status() != ExpenseStatus.PAID) {
            return;
        }

        List<Budget> budgets = budgetRepository.findActiveByTenantIdAndCategoryId(snapshot.tenantId(), snapshot.categoryId());
        if (budgets.isEmpty()) {
            return;
        }

        LocalDate referenceDate = resolveReferenceDate(snapshot.dueDate());
        BigDecimal delta = increase ? snapshot.amount() : snapshot.amount().negate();

        for (Budget budget : budgets) {
            Optional<BudgetPeriodInstance> periodInstance = resolvePeriodInstance(
                    budget,
                    snapshot.tenantId(),
                    referenceDate,
                    allowCreatePeriodInstance
            );
            if (periodInstance.isEmpty()) {
                continue;
            }

            applyDelta(periodInstance.get(), delta);
            triggerNotificationsIfNeeded(periodInstance.get(), snapshot.userId(), snapshot.expenseId(), budget.getName(), delta.signum() > 0);
            budgetPeriodInstanceRepository.save(periodInstance.get());
        }
    }

    public List<BudgetPeriodInstance> ensureHistory(Budget budget, Long tenantId, int monthsBack) {
        if (monthsBack < 1) {
            monthsBack = 1;
        }

        LocalDate today = LocalDate.now();
        Set<LocalDate> includedStartDates = new LinkedHashSet<>();
        List<BudgetPeriodInstance> history = new java.util.ArrayList<>();

        for (int i = 0; i < monthsBack; i++) {
            LocalDate referenceDate = today.minusMonths(i);
            BudgetPeriodInstance period = recalculateForReferenceDate(budget, tenantId, referenceDate, true);
            if (includedStartDates.add(period.getStartDate())) {
                history.add(period);
            }
        }

        history.sort((left, right) -> right.getStartDate().compareTo(left.getStartDate()));
        return history;
    }

    public BudgetPeriodInstance recalculateForReferenceDate(Budget budget, Long tenantId, LocalDate referenceDate, boolean allowCreatePeriodInstance) {
        BudgetPeriodInstance periodInstance = resolvePeriodInstance(budget, tenantId, referenceDate, allowCreatePeriodInstance)
                .orElseThrow(() -> new IllegalStateException("Budget period instance not found for recalculation"));

        BigDecimal spentAmount = expenseRepository.sumByTenantStatusCategoriesAndDueDateRange(
                tenantId,
                ExpenseStatus.PAID,
                budget.getCategories().stream().map(Category::getId).toList(),
                periodInstance.getStartDate(),
                periodInstance.getEndDate());

        periodInstance.setSpentAmount(valueOrZero(spentAmount));
        periodInstance.setStatus(periodInstance.getSpentAmount().compareTo(valueOrZero(periodInstance.getAllocatedAmount())) >= 0
                ? BudgetPeriodStatus.CLOSED
                : BudgetPeriodStatus.OPEN);

        if (periodInstance.getSpentAmount().compareTo(resolveWarningAmount(periodInstance)) < 0) {
            periodInstance.setWarningSent(false);
        }
        if (periodInstance.getSpentAmount().compareTo(valueOrZero(periodInstance.getAllocatedAmount())) <= 0) {
            periodInstance.setExceededSent(false);
        }
        periodInstance.setUpdatedAt(Instant.now());

        return budgetPeriodInstanceRepository.save(periodInstance);
    }

    private LocalDate resolveReferenceDate(LocalDate dueDate) {
        return dueDate != null ? dueDate : LocalDate.now();
    }

    private Optional<BudgetPeriodInstance> resolvePeriodInstance(
            Budget budget,
            Long tenantId,
            LocalDate referenceDate,
            boolean allowCreatePeriodInstance) {
        DateRange dateRange = computeRange(referenceDate, budget.getPeriodicity());

        Optional<BudgetPeriodInstance> existing = budgetPeriodInstanceRepository.findByBudgetIdAndTenantIdAndStartDate(
                budget.getId(),
                tenantId,
                dateRange.startDate()
        );

        if (existing.isPresent()) {
            return existing;
        }

        if (!allowCreatePeriodInstance) {
            return Optional.empty();
        }

        return Optional.of(createPeriodInstance(budget, tenantId, dateRange));
    }

    private BudgetPeriodInstance createPeriodInstance(Budget budget, Long tenantId, DateRange dateRange) {
        BudgetPeriodInstance periodInstance = new BudgetPeriodInstance();
        periodInstance.assignTenantId(tenantId);
        periodInstance.setBudget(budget);
        periodInstance.setStartDate(dateRange.startDate());
        periodInstance.setEndDate(dateRange.endDate());
        periodInstance.setSpentAmount(BigDecimal.ZERO);
        periodInstance.setStatus(BudgetPeriodStatus.OPEN);

        budgetPeriodInstanceRepository.findTopByBudgetIdAndTenantIdAndStartDateBeforeOrderByStartDateDesc(
                budget.getId(),
                tenantId,
                dateRange.startDate()
        ).ifPresentOrElse(previous -> {
            periodInstance.setAllocatedAmount(previous.getAllocatedAmount());
            periodInstance.setWarningType(previous.getWarningType());
            periodInstance.setWarningThreshold(previous.getWarningThreshold());
        }, () -> {
            periodInstance.setAllocatedAmount(budget.getAllocatedAmount());
            periodInstance.setWarningType(budget.getWarningType());
            periodInstance.setWarningThreshold(budget.getWarningThreshold());
        });

        return budgetPeriodInstanceRepository.save(periodInstance);
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
                LocalDate end = LocalDate.of(date.getYear(), 12, 31);
                yield new DateRange(start, end);
            }
        };
    }

    private void applyDelta(BudgetPeriodInstance periodInstance, BigDecimal delta) {
        BigDecimal previousSpent = valueOrZero(periodInstance.getSpentAmount());
        BigDecimal nextSpent = previousSpent.add(delta);
        if (nextSpent.signum() < 0) {
            nextSpent = BigDecimal.ZERO;
        }

        periodInstance.setSpentAmount(nextSpent);
        periodInstance.setStatus(nextSpent.compareTo(valueOrZero(periodInstance.getAllocatedAmount())) >= 0
                ? BudgetPeriodStatus.CLOSED
                : BudgetPeriodStatus.OPEN);

        if (nextSpent.compareTo(resolveWarningAmount(periodInstance)) < 0) {
            periodInstance.setWarningSent(false);
        }
        if (nextSpent.compareTo(valueOrZero(periodInstance.getAllocatedAmount())) <= 0) {
            periodInstance.setExceededSent(false);
        }
        periodInstance.setUpdatedAt(Instant.now());
    }

    private void triggerNotificationsIfNeeded(
            BudgetPeriodInstance periodInstance,
            Long userId,
            Long expenseId,
            String budgetName,
            boolean increasingSpend) {
        if (!increasingSpend || userId == null) {
            return;
        }

        BigDecimal spent = valueOrZero(periodInstance.getSpentAmount());
        BigDecimal warningAmount = resolveWarningAmount(periodInstance);
        BigDecimal allocatedAmount = valueOrZero(periodInstance.getAllocatedAmount());

        if (!periodInstance.isWarningSent() && spent.compareTo(warningAmount) >= 0) {
            notificationService.sendNotification(
                    periodInstance.getTenantId(),
                    userId,
                    NotificationType.BUDGET_ALERT,
                    "Budget warning",
                    "Budget '" + budgetName + "' reached warning threshold in period "
                            + periodInstance.getStartDate() + " to " + periodInstance.getEndDate(),
                    "EXPENSE",
                    expenseId
            );
            periodInstance.setWarningSent(true);
        }

        if (!periodInstance.isExceededSent() && spent.compareTo(allocatedAmount) > 0) {
            notificationService.sendNotification(
                    periodInstance.getTenantId(),
                    userId,
                    NotificationType.BUDGET_EXCEEDED,
                    "Budget exceeded",
                    "Budget '" + budgetName + "' exceeded allocated amount in period "
                            + periodInstance.getStartDate() + " to " + periodInstance.getEndDate(),
                    "EXPENSE",
                    expenseId
            );
            periodInstance.setExceededSent(true);
        }
    }

    private BigDecimal resolveWarningAmount(BudgetPeriodInstance periodInstance) {
        BigDecimal threshold = valueOrZero(periodInstance.getWarningThreshold());
        if (periodInstance.getWarningType() == BudgetWarningType.PERCENTAGE) {
            BigDecimal percentage = threshold.max(BigDecimal.ZERO);
            return valueOrZero(periodInstance.getAllocatedAmount())
                    .multiply(percentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return threshold;
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}

