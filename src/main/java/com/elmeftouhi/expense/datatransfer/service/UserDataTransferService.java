package com.elmeftouhi.expense.datatransfer.service;

import com.elmeftouhi.expense.activity.ActivityAction;
import com.elmeftouhi.expense.activity.ActivitySubjectType;
import com.elmeftouhi.expense.activity.entity.ActivityLog;
import com.elmeftouhi.expense.activity.repository.ActivityLogRepository;
import com.elmeftouhi.expense.budget.entity.Budget;
import com.elmeftouhi.expense.budget.entity.BudgetPeriodInstance;
import com.elmeftouhi.expense.budget.repository.BudgetPeriodInstanceRepository;
import com.elmeftouhi.expense.budget.repository.BudgetRepository;
import com.elmeftouhi.expense.category.entity.Category;
import com.elmeftouhi.expense.category.repository.CategoryRepository;
import com.elmeftouhi.expense.datatransfer.dto.ImportUserDataRequest;
import com.elmeftouhi.expense.datatransfer.dto.ImportUserDataResponse;
import com.elmeftouhi.expense.datatransfer.dto.UserDataBackupSnapshot;
import com.elmeftouhi.expense.expense.entity.Expense;
import com.elmeftouhi.expense.expense.entity.Subscription;
import com.elmeftouhi.expense.expense.entity.SubscriptionPayment;
import com.elmeftouhi.expense.expense.repository.ExpenseRepository;
import com.elmeftouhi.expense.expense.repository.SubscriptionPaymentRepository;
import com.elmeftouhi.expense.expense.repository.SubscriptionRepository;
import com.elmeftouhi.expense.exercice.ExerciceStatus;
import com.elmeftouhi.expense.exercice.entity.Exercice;
import com.elmeftouhi.expense.exercice.repository.ExerciceRepository;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.notification.entity.Notification;
import com.elmeftouhi.expense.notification.repository.NotificationRepository;
import com.elmeftouhi.expense.paymentmethod.entity.PaymentMethod;
import com.elmeftouhi.expense.paymentmethod.repository.PaymentMethodRepository;
import com.elmeftouhi.expense.security.JwtPrincipal;
import com.elmeftouhi.expense.security.TenantPermissionService;
import com.elmeftouhi.expense.shared.exception.BadRequestException;
import com.elmeftouhi.expense.shared.exception.ConflictException;
import com.elmeftouhi.expense.shared.exception.ResourceNotFoundException;
import com.elmeftouhi.expense.tenant.entity.Tenant;
import com.elmeftouhi.expense.tenant.repository.TenantRepository;
import com.elmeftouhi.expense.tenantconfig.entity.TenantConfiguration;
import com.elmeftouhi.expense.tenantconfig.repository.TenantConfigurationRepository;
import com.elmeftouhi.expense.tenantconfig.service.TenantConfigurationService;
import com.elmeftouhi.expense.user.TenantRole;
import com.elmeftouhi.expense.user.entity.AppUser;
import com.elmeftouhi.expense.user.entity.UserTenant;
import com.elmeftouhi.expense.user.repository.AppUserRepository;
import com.elmeftouhi.expense.user.repository.UserTenantRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDataTransferService {

    private static final String FORMAT_VERSION = "1.0";

    private final TenantPermissionService tenantPermissionService;
    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final UserTenantRepository userTenantRepository;
    private final ExerciceRepository exerciceRepository;
    private final CategoryRepository categoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetPeriodInstanceRepository budgetPeriodInstanceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ExpenseRepository expenseRepository;
    private final SubscriptionPaymentRepository subscriptionPaymentRepository;
    private final TenantConfigurationRepository tenantConfigurationRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final TenantConfigurationService tenantConfigurationService;

    @Transactional(readOnly = true)
    public UserDataBackupSnapshot exportCurrentTenantData() {
        tenantPermissionService.requireAdmin();

        Long tenantId = TenantContext.getRequiredTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        JwtPrincipal principal = getRequiredPrincipal();

        UserDataBackupSnapshot.BackupMetadata metadata = new UserDataBackupSnapshot.BackupMetadata(
                FORMAT_VERSION,
                Instant.now(),
                tenantId,
                tenant.getName(),
                principal.userId(),
                principal.username());

        UserDataBackupSnapshot.TenantConfigurationEntry tenantConfiguration = tenantConfigurationRepository
                .findByTenantId(tenantId)
                .map(this::toTenantConfigurationEntry)
                .orElse(null);

        List<UserDataBackupSnapshot.ExerciceEntry> exercices = exerciceRepository.findAllByTenantIdOrderByStartDateDesc(tenantId)
                .stream()
                .map(this::toExerciceEntry)
                .toList();

        List<UserDataBackupSnapshot.CategoryEntry> categories = categoryRepository.findByTenantIdOrderByLevelAscNameAscIdAsc(tenantId)
                .stream()
                .map(this::toCategoryEntry)
                .toList();

        List<UserDataBackupSnapshot.PaymentMethodEntry> paymentMethods = paymentMethodRepository
                .findByTenantIdOrderByIsDefaultDescNameAsc(tenantId)
                .stream()
                .map(this::toPaymentMethodEntry)
                .toList();

        List<UserDataBackupSnapshot.BudgetEntry> budgets = budgetRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::toBudgetEntry)
                .toList();

        List<UserDataBackupSnapshot.BudgetPeriodInstanceEntry> budgetPeriodInstances = budgetPeriodInstanceRepository
                .findByTenantIdOrderByStartDateAscIdAsc(tenantId)
                .stream()
                .map(this::toBudgetPeriodInstanceEntry)
                .toList();

        List<UserDataBackupSnapshot.SubscriptionEntry> subscriptions = subscriptionRepository
                .findByTenantIdOrderByCreatedAtAscIdAsc(tenantId)
                .stream()
                .map(this::toSubscriptionEntry)
                .toList();

        List<UserDataBackupSnapshot.ExpenseEntry> expenses = expenseRepository.findByTenantIdOrderByDueDateDesc(tenantId)
                .stream()
                .map(this::toExpenseEntry)
                .toList();

        List<UserDataBackupSnapshot.SubscriptionPaymentEntry> subscriptionPayments = subscriptionPaymentRepository
                .findByTenantIdOrderByScheduledBillingDateAscIdAsc(tenantId)
                .stream()
                .map(this::toSubscriptionPaymentEntry)
                .toList();

        List<UserDataBackupSnapshot.NotificationEntry> notifications = notificationRepository.findByTenantIdOrderByCreatedAtAscIdAsc(tenantId)
                .stream()
                .map(this::toNotificationEntry)
                .toList();

        List<UserDataBackupSnapshot.ActivityLogEntry> activityLogs = activityLogRepository.findByTenantIdOrderByOccurredAtAscIdAsc(tenantId)
                .stream()
                .map(this::toActivityLogEntry)
                .toList();

        return new UserDataBackupSnapshot(
                metadata,
                tenantConfiguration,
                exercices,
                categories,
                paymentMethods,
                budgets,
                budgetPeriodInstances,
                subscriptions,
                expenses,
                subscriptionPayments,
                notifications,
                activityLogs);
    }

    @Transactional
    public ImportUserDataResponse importIntoNewTenant(ImportUserDataRequest request) {
        JwtPrincipal principal = getRequiredPrincipal();
        AppUser importer = appUserRepository.findWithDefaultTenantById(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String normalizedTenantName = request.tenantName().trim();
        if (tenantRepository.existsByNameIgnoreCase(normalizedTenantName)) {
            throw new ConflictException("Tenant name already exists");
        }

        UserDataBackupSnapshot backup = request.backup();
        validateBackup(backup);

        Tenant newTenant = new Tenant();
        newTenant.setName(normalizedTenantName);
        newTenant = tenantRepository.save(newTenant);

        UserTenant membership = new UserTenant();
        membership.setUser(importer);
        membership.setTenant(newTenant);
        membership.setTenantRole(TenantRole.ADMIN);
        userTenantRepository.save(membership);

        if (request.switchDefaultTenant()) {
            importer.setDefaultTenant(newTenant);
            importer.setUpdatedAt(Instant.now());
            appUserRepository.save(importer);
        }

        ImportContext importContext = new ImportContext(principal.userId(), newTenant.getId());
        Long previousTenantId = TenantContext.getTenantId();

        try {
            TenantContext.setTenantId(newTenant.getId());

            importExercices(nullSafeList(backup.exercices()), importContext);
            importTenantConfiguration(backup.tenantConfiguration(), principal.username());
            importCategories(nullSafeList(backup.categories()), importContext);
            importPaymentMethods(nullSafeList(backup.paymentMethods()), importContext);
            importBudgets(nullSafeList(backup.budgets()), importContext);
            importBudgetPeriodInstances(nullSafeList(backup.budgetPeriodInstances()), importContext);
            importSubscriptions(nullSafeList(backup.subscriptions()), importContext);
            importExpenses(nullSafeList(backup.expenses()), importContext);
            importSubscriptionPayments(nullSafeList(backup.subscriptionPayments()), importContext);
            importNotifications(nullSafeList(backup.notifications()), importContext);
            importActivityLogs(nullSafeList(backup.activityLogs()), importContext);

            ensureCurrentYearExerciceExists(newTenant.getId());
            tenantConfigurationService.invalidateCache(newTenant.getId());
        } finally {
            restoreTenantContext(previousTenantId);
        }

        ImportUserDataResponse.ImportCounts counts = new ImportUserDataResponse.ImportCounts(
                importContext.importedExercices,
                importContext.importedCategories,
                importContext.importedPaymentMethods,
                importContext.importedBudgets,
                importContext.importedBudgetPeriodInstances,
                importContext.importedSubscriptions,
                importContext.importedExpenses,
                importContext.importedSubscriptionPayments,
                importContext.importedNotifications,
                importContext.importedActivityLogs);

        return new ImportUserDataResponse(newTenant.getId(), newTenant.getName(), request.switchDefaultTenant(), counts);
    }

    private void validateBackup(UserDataBackupSnapshot backup) {
        if (backup == null) {
            throw new BadRequestException("backup is required");
        }

        UserDataBackupSnapshot.BackupMetadata metadata = backup.metadata();
        if (metadata == null || metadata.formatVersion() == null || metadata.formatVersion().isBlank()) {
            throw new BadRequestException("backup metadata with formatVersion is required");
        }

        if (!Objects.equals(metadata.formatVersion(), FORMAT_VERSION)) {
            throw new BadRequestException("Unsupported backup format version: " + metadata.formatVersion());
        }
    }

    private void importTenantConfiguration(UserDataBackupSnapshot.TenantConfigurationEntry entry, String username) {
        if (entry == null) {
            return;
        }

        TenantConfiguration configuration = new TenantConfiguration();
        configuration.assignTenantId(TenantContext.getRequiredTenantId());
        configuration.setCurrency(entry.currency());
        configuration.setDecimalPlaces(entry.decimalPlaces());
        configuration.setLanguage(entry.language());
        configuration.setTimezone(entry.timezone());
        configuration.setDateFormat(entry.dateFormat());
        configuration.setTimeFormat(entry.timeFormat());
        configuration.setWeekStartDay(entry.weekStartDay());
        configuration.setBudgetAlertThreshold(entry.budgetAlertThreshold());
        configuration.setNotificationChannels(entry.notificationChannels());
        configuration.setAutoPaymentReminderDays(entry.autoPaymentReminderDays());
        configuration.setExpenseRoundingMethod(entry.expenseRoundingMethod());
        configuration.setReportingPeriod(entry.reportingPeriod());
        configuration.setCreatedAt(defaultIfNull(entry.createdAt(), Instant.now()));
        configuration.setUpdatedAt(defaultIfNull(entry.updatedAt(), Instant.now()));
        configuration.setCreatedBy(entry.createdBy() != null ? entry.createdBy() : username);
        configuration.setUpdatedBy(entry.updatedBy() != null ? entry.updatedBy() : username);
        tenantConfigurationRepository.save(configuration);
    }

    private void importExercices(List<UserDataBackupSnapshot.ExerciceEntry> entries, ImportContext context) {
        for (UserDataBackupSnapshot.ExerciceEntry entry : entries) {
            Exercice exercice = new Exercice();
            exercice.assignTenantId(context.targetTenantId);
            exercice.setName(entry.name());
            exercice.setStartDate(entry.startDate());
            exercice.setEndDate(entry.endDate());
            exercice.setStatus(defaultIfNull(entry.status(), ExerciceStatus.OPEN));
            exercice.setCreatedAt(defaultIfNull(entry.createdAt(), Instant.now()));
            exercice.setUpdatedAt(defaultIfNull(entry.updatedAt(), Instant.now()));

            Exercice saved = exerciceRepository.save(exercice);
            if (entry.legacyId() != null) {
                context.exerciceMap.put(entry.legacyId(), saved);
            }
            context.importedExercices++;
        }
    }

    private void importCategories(List<UserDataBackupSnapshot.CategoryEntry> entries, ImportContext context) {
        List<UserDataBackupSnapshot.CategoryEntry> pending = new ArrayList<>(entries);
        pending.sort(Comparator
                .comparing((UserDataBackupSnapshot.CategoryEntry entry) -> defaultIfNull(entry.level(), 0))
                .thenComparing(entry -> entry.name() == null ? "" : entry.name(), String.CASE_INSENSITIVE_ORDER));

        while (!pending.isEmpty()) {
            int importedInPass = 0;
            List<UserDataBackupSnapshot.CategoryEntry> nextPending = new ArrayList<>();

            for (UserDataBackupSnapshot.CategoryEntry entry : pending) {
                if (entry.parentLegacyId() != null && !context.categoryMap.containsKey(entry.parentLegacyId())) {
                    nextPending.add(entry);
                    continue;
                }

                Category category = new Category();
                category.assignTenantId(context.targetTenantId);
                category.setParentCategory(entry.parentLegacyId() != null ? context.categoryMap.get(entry.parentLegacyId()) : null);
                category.setLevel(defaultIfNull(entry.level(), 0));
                category.setName(entry.name());
                category.setDescription(entry.description());
                category.setIcon(entry.icon());
                category.setTextColor(entry.textColor());
                category.setBackgroundColor(entry.backgroundColor());
                category.setMonthlyBudget(entry.monthlyBudget());
                category.setActive(entry.active());
                category.setAllowOverBudget(entry.allowOverBudget());

                Category saved = categoryRepository.save(category);
                if (entry.legacyId() != null) {
                    context.categoryMap.put(entry.legacyId(), saved);
                }
                context.importedCategories++;
                importedInPass++;
            }

            if (importedInPass == 0) {
                throw new BadRequestException("Category hierarchy is invalid in backup payload");
            }
            pending = nextPending;
        }
    }

    private void importPaymentMethods(List<UserDataBackupSnapshot.PaymentMethodEntry> entries, ImportContext context) {
        for (UserDataBackupSnapshot.PaymentMethodEntry entry : entries) {
            PaymentMethod paymentMethod = new PaymentMethod();
            paymentMethod.assignTenantId(context.targetTenantId);
            paymentMethod.setName(entry.name());
            paymentMethod.setType(entry.type());
            paymentMethod.setDefault(entry.isDefault());
            paymentMethod.setActive(entry.active());
            paymentMethod.setCreatedAt(defaultIfNull(entry.createdAt(), Instant.now()));
            paymentMethod.setUpdatedAt(defaultIfNull(entry.updatedAt(), Instant.now()));

            PaymentMethod saved = paymentMethodRepository.save(paymentMethod);
            if (entry.legacyId() != null) {
                context.paymentMethodMap.put(entry.legacyId(), saved);
            }
            context.importedPaymentMethods++;
        }
    }

    private void importBudgets(List<UserDataBackupSnapshot.BudgetEntry> entries, ImportContext context) {
        for (UserDataBackupSnapshot.BudgetEntry entry : entries) {
            Budget budget = new Budget();
            budget.assignTenantId(context.targetTenantId);
            budget.setName(entry.name());
            budget.setDescription(entry.description());
            budget.setPeriodicity(entry.periodicity());
            budget.setAllocatedAmount(entry.allocatedAmount());
            budget.setWarningType(entry.warningType());
            budget.setWarningThreshold(entry.warningThreshold());
            budget.setActive(entry.active());
            budget.setCreatedAt(defaultIfNull(entry.createdAt(), Instant.now()));
            budget.setUpdatedAt(defaultIfNull(entry.updatedAt(), Instant.now()));

            for (Long categoryLegacyId : nullSafeList(entry.categoryLegacyIds())) {
                Category category = context.categoryMap.get(categoryLegacyId);
                if (category == null) {
                    throw new BadRequestException("Budget references unknown category legacy id: " + categoryLegacyId);
                }
                budget.getCategories().add(category);
            }

            Budget saved = budgetRepository.save(budget);
            if (entry.legacyId() != null) {
                context.budgetMap.put(entry.legacyId(), saved);
            }
            context.importedBudgets++;
        }
    }

    private void importBudgetPeriodInstances(List<UserDataBackupSnapshot.BudgetPeriodInstanceEntry> entries, ImportContext context) {
        for (UserDataBackupSnapshot.BudgetPeriodInstanceEntry entry : entries) {
            Budget budget = context.budgetMap.get(entry.budgetLegacyId());
            if (budget == null) {
                throw new BadRequestException("Budget period instance references unknown budget legacy id: " + entry.budgetLegacyId());
            }

            BudgetPeriodInstance instance = new BudgetPeriodInstance();
            instance.assignTenantId(context.targetTenantId);
            instance.setBudget(budget);
            instance.setStartDate(entry.startDate());
            instance.setEndDate(entry.endDate());
            instance.setAllocatedAmount(entry.allocatedAmount());
            instance.setSpentAmount(entry.spentAmount());
            instance.setStatus(entry.status());
            instance.setWarningType(entry.warningType());
            instance.setWarningThreshold(entry.warningThreshold());
            instance.setWarningSent(entry.warningSent());
            instance.setExceededSent(entry.exceededSent());
            instance.setCreatedAt(defaultIfNull(entry.createdAt(), Instant.now()));
            instance.setUpdatedAt(defaultIfNull(entry.updatedAt(), Instant.now()));

            BudgetPeriodInstance saved = budgetPeriodInstanceRepository.save(instance);
            if (entry.legacyId() != null) {
                context.budgetPeriodMap.put(entry.legacyId(), saved);
            }
            context.importedBudgetPeriodInstances++;
        }
    }

    private void importSubscriptions(List<UserDataBackupSnapshot.SubscriptionEntry> entries, ImportContext context) {
        for (UserDataBackupSnapshot.SubscriptionEntry entry : entries) {
            Category category = context.categoryMap.get(entry.categoryLegacyId());
            if (category == null) {
                throw new BadRequestException("Subscription references unknown category legacy id: " + entry.categoryLegacyId());
            }

            Subscription subscription = new Subscription();
            subscription.assignTenantId(context.targetTenantId);
            subscription.setName(entry.name());
            subscription.setDescription(entry.description());
            subscription.setCategory(category);
            subscription.setOwnerUserId(context.resolveUserId(entry.ownerUserId()));
            subscription.setAmount(entry.amount());
            subscription.setCurrency(entry.currency());
            subscription.setFrequency(entry.frequency());
            subscription.setCustomIntervalDays(entry.customIntervalDays());
            subscription.setNextBillingDate(entry.nextBillingDate());
            subscription.setEndDate(entry.endDate());
            subscription.setStatus(entry.status());
            subscription.setReminderLeadDays(entry.reminderLeadDays());
            subscription.setPaymentMethod(resolvePaymentMethod(entry.paymentMethodLegacyId(), context));
            subscription.setNotes(entry.notes());
            subscription.setAnchorDayOfMonth(entry.anchorDayOfMonth());
            subscription.setAnchorMonthOfYear(entry.anchorMonthOfYear());
            subscription.setCreatedAt(defaultIfNull(entry.createdAt(), Instant.now()));
            subscription.setUpdatedAt(defaultIfNull(entry.updatedAt(), Instant.now()));
            subscription.setVersion(defaultIfNull(entry.version(), 0L));

            Subscription saved = subscriptionRepository.save(subscription);
            if (entry.legacyId() != null) {
                context.subscriptionMap.put(entry.legacyId(), saved);
            }
            context.importedSubscriptions++;
        }
    }

    private void importExpenses(List<UserDataBackupSnapshot.ExpenseEntry> entries, ImportContext context) {
        for (UserDataBackupSnapshot.ExpenseEntry entry : entries) {
            Category category = context.categoryMap.get(entry.categoryLegacyId());
            if (category == null) {
                throw new BadRequestException("Expense references unknown category legacy id: " + entry.categoryLegacyId());
            }

            Expense expense = new Expense();
            expense.assignTenantId(context.targetTenantId);
            expense.setCategory(category);
            expense.setUserId(context.resolveUserId(entry.userId()));
            expense.setDescription(entry.description());
            expense.setAmount(entry.amount());
            expense.setCurrency(entry.currency());
            expense.setType(entry.type());
            expense.setStatus(entry.status());
            expense.setDueDate(entry.dueDate());
            expense.setPaidDate(entry.paidDate());
            expense.setPaymentMethod(resolvePaymentMethod(entry.paymentMethodLegacyId(), context));
            expense.setNotes(entry.notes());
            expense.setDeleted(defaultIfNull(entry.deleted(), Boolean.FALSE));
            expense.setCreatedAt(defaultIfNull(entry.createdAt(), Instant.now()));
            expense.setUpdatedAt(defaultIfNull(entry.updatedAt(), Instant.now()));
            expense.setDeletedAt(entry.deletedAt());

            Expense saved = expenseRepository.save(expense);
            if (entry.legacyId() != null) {
                context.expenseMap.put(entry.legacyId(), saved);
            }
            context.importedExpenses++;
        }
    }

    private void importSubscriptionPayments(List<UserDataBackupSnapshot.SubscriptionPaymentEntry> entries, ImportContext context) {
        for (UserDataBackupSnapshot.SubscriptionPaymentEntry entry : entries) {
            Subscription subscription = context.subscriptionMap.get(entry.subscriptionLegacyId());
            Expense expense = context.expenseMap.get(entry.expenseLegacyId());
            if (subscription == null || expense == null) {
                throw new BadRequestException("Subscription payment references unknown legacy ids");
            }

            SubscriptionPayment payment = new SubscriptionPayment();
            payment.assignTenantId(context.targetTenantId);
            payment.setSubscription(subscription);
            payment.setExpense(expense);
            payment.setScheduledBillingDate(entry.scheduledBillingDate());
            payment.setCreatedAt(defaultIfNull(entry.createdAt(), Instant.now()));
            subscriptionPaymentRepository.save(payment);
            context.importedSubscriptionPayments++;
        }
    }

    private void importNotifications(List<UserDataBackupSnapshot.NotificationEntry> entries, ImportContext context) {
        for (UserDataBackupSnapshot.NotificationEntry entry : entries) {
            Notification notification = new Notification();
            notification.assignTenantId(context.targetTenantId);
            notification.setUserId(context.resolveUserId(entry.userId()));
            notification.setType(entry.type());
            notification.setStatus(entry.status());
            notification.setTitle(entry.title());
            notification.setMessage(entry.message());
            notification.setSubjectType(entry.subjectType());
            notification.setSubjectId(remapSubjectId(entry.subjectType(), entry.subjectId(), context));
            notification.setData(entry.data());
            notification.setCreatedAt(defaultIfNull(entry.createdAt(), Instant.now()));
            notification.setReadAt(entry.readAt());
            notification.setArchivedAt(entry.archivedAt());
            notificationRepository.save(notification);
            context.importedNotifications++;
        }
    }

    private void importActivityLogs(List<UserDataBackupSnapshot.ActivityLogEntry> entries, ImportContext context) {
        for (UserDataBackupSnapshot.ActivityLogEntry entry : entries) {
            ActivityLog log = new ActivityLog();
            log.assignTenantId(context.targetTenantId);
            log.setSubjectType(entry.subjectType());
            log.setSubjectId(remapSubjectId(entry.subjectType() != null ? entry.subjectType().name() : null, entry.subjectId(), context));
            log.setSubjectLabel(entry.subjectLabel());
            log.setAction(entry.action());
            log.setActorUsername(defaultIfBlank(entry.actorUsername(), "system"));
            log.setMessage(entry.message());
            log.setMetadata(entry.metadata());
            log.setOccurredAt(defaultIfNull(entry.occurredAt(), Instant.now()));
            activityLogRepository.save(log);
            context.importedActivityLogs++;
        }

        if (!entries.isEmpty()) {
            ActivityLog importedLog = new ActivityLog();
            importedLog.assignTenantId(context.targetTenantId);
            importedLog.setSubjectType(ActivitySubjectType.TENANT);
            importedLog.setSubjectId(context.targetTenantId);
            importedLog.setSubjectLabel("Imported workspace");
            importedLog.setAction(ActivityAction.CREATE);
            importedLog.setActorUsername("system");
            importedLog.setMessage("Data import completed");
            importedLog.setMetadata("{\"source\":\"backup\"}");
            importedLog.setOccurredAt(Instant.now());
            activityLogRepository.save(importedLog);
        }
    }

    private Long remapSubjectId(String subjectType, Long subjectId, ImportContext context) {
        if (subjectType == null || subjectId == null) {
            return subjectId;
        }

        String normalized = subjectType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "EXPENSE" -> context.expenseMap.get(subjectId) != null ? context.expenseMap.get(subjectId).getId() : subjectId;
            case "SUBSCRIPTION" -> context.subscriptionMap.get(subjectId) != null ? context.subscriptionMap.get(subjectId).getId() : subjectId;
            case "BUDGET" -> context.budgetMap.get(subjectId) != null ? context.budgetMap.get(subjectId).getId() : subjectId;
            case "CATEGORY" -> context.categoryMap.get(subjectId) != null ? context.categoryMap.get(subjectId).getId() : subjectId;
            default -> subjectId;
        };
    }

    private PaymentMethod resolvePaymentMethod(Long legacyPaymentMethodId, ImportContext context) {
        if (legacyPaymentMethodId == null) {
            return null;
        }
        PaymentMethod method = context.paymentMethodMap.get(legacyPaymentMethodId);
        if (method == null) {
            throw new BadRequestException("Unknown payment method legacy id: " + legacyPaymentMethodId);
        }
        return method;
    }

    private void ensureCurrentYearExerciceExists(Long tenantId) {
        LocalDate today = LocalDate.now();
        if (exerciceRepository.findExerciceForDate(tenantId, today).isPresent()) {
            return;
        }

        Exercice exercice = new Exercice();
        exercice.assignTenantId(tenantId);
        exercice.setName("Exercice " + today.getYear());
        exercice.setStartDate(LocalDate.of(today.getYear(), 1, 1));
        exercice.setEndDate(LocalDate.of(today.getYear(), 12, 31));
        exercice.setStatus(ExerciceStatus.OPEN);
        exerciceRepository.save(exercice);
    }

    private void restoreTenantContext(Long previousTenantId) {
        if (previousTenantId == null) {
            TenantContext.clear();
        } else {
            TenantContext.setTenantId(previousTenantId);
        }
    }

    private JwtPrincipal getRequiredPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new BadRequestException("Authenticated user not found");
        }
        return principal;
    }

    private UserDataBackupSnapshot.TenantConfigurationEntry toTenantConfigurationEntry(TenantConfiguration config) {
        return new UserDataBackupSnapshot.TenantConfigurationEntry(
                config.getCurrency(),
                config.getDecimalPlaces(),
                config.getLanguage(),
                config.getTimezone(),
                config.getDateFormat(),
                config.getTimeFormat(),
                config.getWeekStartDay(),
                config.getBudgetAlertThreshold(),
                config.getNotificationChannels(),
                config.getAutoPaymentReminderDays(),
                config.getExpenseRoundingMethod(),
                config.getReportingPeriod(),
                config.getCreatedAt(),
                config.getUpdatedAt(),
                config.getCreatedBy(),
                config.getUpdatedBy());
    }

    private UserDataBackupSnapshot.ExerciceEntry toExerciceEntry(Exercice exercice) {
        return new UserDataBackupSnapshot.ExerciceEntry(
                exercice.getId(),
                exercice.getName(),
                exercice.getStartDate(),
                exercice.getEndDate(),
                exercice.getStatus(),
                exercice.getCreatedAt(),
                exercice.getUpdatedAt());
    }

    private UserDataBackupSnapshot.CategoryEntry toCategoryEntry(Category category) {
        return new UserDataBackupSnapshot.CategoryEntry(
                category.getId(),
                category.getParentCategory() != null ? category.getParentCategory().getId() : null,
                category.getLevel(),
                category.getName(),
                category.getDescription(),
                category.getIcon(),
                category.getTextColor(),
                category.getBackgroundColor(),
                category.getMonthlyBudget(),
                category.isActive(),
                category.isAllowOverBudget());
    }

    private UserDataBackupSnapshot.PaymentMethodEntry toPaymentMethodEntry(PaymentMethod paymentMethod) {
        return new UserDataBackupSnapshot.PaymentMethodEntry(
                paymentMethod.getId(),
                paymentMethod.getName(),
                paymentMethod.getType(),
                paymentMethod.isDefault(),
                paymentMethod.isActive(),
                paymentMethod.getCreatedAt(),
                paymentMethod.getUpdatedAt());
    }

    private UserDataBackupSnapshot.BudgetEntry toBudgetEntry(Budget budget) {
        return new UserDataBackupSnapshot.BudgetEntry(
                budget.getId(),
                budget.getName(),
                budget.getDescription(),
                budget.getPeriodicity(),
                budget.getAllocatedAmount(),
                budget.getWarningType(),
                budget.getWarningThreshold(),
                budget.isActive(),
                budget.getCategories().stream().map(Category::getId).toList(),
                budget.getCreatedAt(),
                budget.getUpdatedAt());
    }

    private UserDataBackupSnapshot.BudgetPeriodInstanceEntry toBudgetPeriodInstanceEntry(BudgetPeriodInstance instance) {
        return new UserDataBackupSnapshot.BudgetPeriodInstanceEntry(
                instance.getId(),
                instance.getBudget().getId(),
                instance.getStartDate(),
                instance.getEndDate(),
                instance.getAllocatedAmount(),
                instance.getSpentAmount(),
                instance.getStatus(),
                instance.getWarningType(),
                instance.getWarningThreshold(),
                instance.isWarningSent(),
                instance.isExceededSent(),
                instance.getCreatedAt(),
                instance.getUpdatedAt());
    }

    private UserDataBackupSnapshot.SubscriptionEntry toSubscriptionEntry(Subscription subscription) {
        return new UserDataBackupSnapshot.SubscriptionEntry(
                subscription.getId(),
                subscription.getName(),
                subscription.getDescription(),
                subscription.getCategory().getId(),
                subscription.getOwnerUserId(),
                subscription.getAmount(),
                subscription.getCurrency(),
                subscription.getFrequency(),
                subscription.getCustomIntervalDays(),
                subscription.getNextBillingDate(),
                subscription.getEndDate(),
                subscription.getStatus(),
                subscription.getReminderLeadDays(),
                subscription.getPaymentMethod() != null ? subscription.getPaymentMethod().getId() : null,
                subscription.getNotes(),
                subscription.getAnchorDayOfMonth(),
                subscription.getAnchorMonthOfYear(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt(),
                subscription.getVersion());
    }

    private UserDataBackupSnapshot.ExpenseEntry toExpenseEntry(Expense expense) {
        return new UserDataBackupSnapshot.ExpenseEntry(
                expense.getId(),
                expense.getCategory().getId(),
                expense.getUserId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getCurrency(),
                expense.getType(),
                expense.getStatus(),
                expense.getDueDate(),
                expense.getPaidDate(),
                expense.getPaymentMethod() != null ? expense.getPaymentMethod().getId() : null,
                expense.getNotes(),
                expense.getDeleted(),
                expense.getCreatedAt(),
                expense.getUpdatedAt(),
                expense.getDeletedAt());
    }

    private UserDataBackupSnapshot.SubscriptionPaymentEntry toSubscriptionPaymentEntry(SubscriptionPayment payment) {
        return new UserDataBackupSnapshot.SubscriptionPaymentEntry(
                payment.getId(),
                payment.getSubscription().getId(),
                payment.getExpense().getId(),
                payment.getScheduledBillingDate(),
                payment.getCreatedAt());
    }

    private UserDataBackupSnapshot.NotificationEntry toNotificationEntry(Notification notification) {
        return new UserDataBackupSnapshot.NotificationEntry(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getStatus(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getSubjectType(),
                notification.getSubjectId(),
                notification.getData(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                notification.getArchivedAt());
    }

    private UserDataBackupSnapshot.ActivityLogEntry toActivityLogEntry(ActivityLog log) {
        return new UserDataBackupSnapshot.ActivityLogEntry(
                log.getId(),
                log.getSubjectType(),
                log.getSubjectId(),
                log.getSubjectLabel(),
                log.getAction(),
                log.getActorUsername(),
                log.getMessage(),
                log.getMetadata(),
                log.getOccurredAt());
    }

    private static <T> T defaultIfNull(T value, T fallback) {
        return value != null ? value : fallback;
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static <T> List<T> nullSafeList(Collection<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static final class ImportContext {
        private final Long importingUserId;
        private final Long targetTenantId;
        private final Map<Long, Exercice> exerciceMap = new HashMap<>();
        private final Map<Long, Category> categoryMap = new HashMap<>();
        private final Map<Long, PaymentMethod> paymentMethodMap = new HashMap<>();
        private final Map<Long, Budget> budgetMap = new HashMap<>();
        private final Map<Long, BudgetPeriodInstance> budgetPeriodMap = new HashMap<>();
        private final Map<Long, Subscription> subscriptionMap = new HashMap<>();
        private final Map<Long, Expense> expenseMap = new HashMap<>();

        private int importedExercices;
        private int importedCategories;
        private int importedPaymentMethods;
        private int importedBudgets;
        private int importedBudgetPeriodInstances;
        private int importedSubscriptions;
        private int importedExpenses;
        private int importedSubscriptionPayments;
        private int importedNotifications;
        private int importedActivityLogs;

        private ImportContext(Long importingUserId, Long targetTenantId) {
            this.importingUserId = importingUserId;
            this.targetTenantId = targetTenantId;
        }

        private Long resolveUserId(Long sourceUserId) {
            return importingUserId;
        }
    }
}

