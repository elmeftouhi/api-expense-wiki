package com.elmeftouhi.expense.expense.service;

import com.elmeftouhi.expense.expense.entity.Subscription;
import com.elmeftouhi.expense.expense.repository.SubscriptionRepository;
import com.elmeftouhi.expense.expense.SubscriptionFrequency;
import com.elmeftouhi.expense.expense.SubscriptionLifecycleStatus;
import com.elmeftouhi.expense.activity.ActivityAction;
import com.elmeftouhi.expense.activity.service.ActivityLogService;
import com.elmeftouhi.expense.activity.ActivitySubjectType;
import com.elmeftouhi.expense.category.entity.Category;
import com.elmeftouhi.expense.category.repository.CategoryRepository;
import com.elmeftouhi.expense.expense.dto.SubscriptionCreateRequest;
import com.elmeftouhi.expense.expense.dto.SubscriptionResponse;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.paymentmethod.entity.PaymentMethod;
import com.elmeftouhi.expense.paymentmethod.service.PaymentMethodService;
import com.elmeftouhi.expense.security.JwtPrincipal;
import com.elmeftouhi.expense.security.TenantPermissionService;
import com.elmeftouhi.expense.shared.exception.BadRequestException;
import com.elmeftouhi.expense.shared.exception.ResourceNotFoundException;
import com.elmeftouhi.expense.tenantconfig.ConfigurationContext;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final CategoryRepository categoryRepository;
    private final ActivityLogService activityLogService;
    private final PaymentMethodService paymentMethodService;
    private final TenantPermissionService tenantPermissionService;

    @Transactional
    public SubscriptionResponse create(SubscriptionCreateRequest request) {
        tenantPermissionService.requireEditor();
        Long tenantId = TenantContext.getRequiredTenantId();
        Long currentUserId = getCurrentUserId();

        Category category = loadCategory(request.categoryId(), tenantId);
        PaymentMethod paymentMethod = paymentMethodService.resolveOptionalForUsage(tenantId, request.paymentMethodId());
        validateRequest(request);
        String configuredCurrency = ConfigurationContext.getCurrency().name();
        if (request.currency() != null && !request.currency().equalsIgnoreCase(configuredCurrency)) {
            throw new BadRequestException("Subscription currency must match tenant currency policy: " + configuredCurrency);
        }

        Subscription subscription = new Subscription();
        subscription.assignTenantId(tenantId);
        subscription.setName(request.name().trim());
        subscription.setDescription(request.description());
        subscription.setCategory(category);
        subscription.setOwnerUserId(currentUserId);
        subscription.setAmount(request.amount());
        subscription.setCurrency(configuredCurrency);
        subscription.setFrequency(request.frequency());
        subscription.setCustomIntervalDays(request.customIntervalDays());
        subscription.setNextBillingDate(request.nextBillingDate());
        subscription.setEndDate(request.endDate());
        subscription.setStatus(SubscriptionLifecycleStatus.ACTIVE);
        subscription.setReminderLeadDays(resolveReminderLeadDays(request.reminderLeadDays()));
        subscription.setPaymentMethod(paymentMethod);
        subscription.setNotes(request.notes());
        subscription.refreshAnchorsFromNextBillingDate();

        Subscription saved = subscriptionRepository.save(subscription);
        logActivity(ActivityAction.CREATE, saved, "Subscription created");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> search(
            String search,
            SubscriptionLifecycleStatus status,
            List<Long> categoryIds,
            SubscriptionFrequency frequency,
            LocalDate nextBillingFrom,
            LocalDate nextBillingTo,
            Pageable pageable) {
        Long tenantId = TenantContext.getRequiredTenantId();

        if (nextBillingFrom != null && nextBillingTo != null && nextBillingTo.isBefore(nextBillingFrom)) {
            throw new BadRequestException("nextBillingTo must be greater than or equal to nextBillingFrom");
        }

        String normalizedSearch = normalizeSearch(search);
        List<Long> normalizedCategoryIds = normalizeCategoryIds(categoryIds);
        boolean applyCategoryFilter = normalizedCategoryIds != null;

        if (normalizedSearch == null) {
            return subscriptionRepository.searchWithoutText(
                            tenantId,
                            status,
                            applyCategoryFilter,
                            normalizedCategoryIds,
                            frequency,
                            nextBillingFrom,
                            nextBillingTo,
                            pageable)
                    .map(this::toResponse);
        }

        return subscriptionRepository.search(
                        tenantId,
                        normalizedSearch,
                        status,
                        applyCategoryFilter,
                        normalizedCategoryIds,
                        frequency,
                        nextBillingFrom,
                        nextBillingTo,
                        pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getById(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Subscription subscription = loadSubscription(id, tenantId);
        return toResponse(subscription);
    }

    @Transactional
    public SubscriptionResponse update(Long id, SubscriptionCreateRequest request) {
        tenantPermissionService.requireEditor();
        Long tenantId = TenantContext.getRequiredTenantId();
        Subscription subscription = loadSubscription(id, tenantId);

        if (subscription.getStatus() == SubscriptionLifecycleStatus.STOPPED
                || subscription.getStatus() == SubscriptionLifecycleStatus.ENDED) {
            throw new BadRequestException("Stopped or ended subscriptions cannot be edited");
        }

        Category category = loadCategory(request.categoryId(), tenantId);
        PaymentMethod paymentMethod = paymentMethodService.resolveOptionalForUsage(tenantId, request.paymentMethodId());
        validateRequest(request);
        String configuredCurrency = ConfigurationContext.getCurrency().name();
        if (request.currency() != null && !request.currency().equalsIgnoreCase(configuredCurrency)) {
            throw new BadRequestException("Subscription currency must match tenant currency policy: " + configuredCurrency);
        }

        subscription.setName(request.name().trim());
        subscription.setDescription(request.description());
        subscription.setCategory(category);
        subscription.setAmount(request.amount());
        subscription.setCurrency(configuredCurrency);
        subscription.setFrequency(request.frequency());
        subscription.setCustomIntervalDays(request.customIntervalDays());
        subscription.setNextBillingDate(request.nextBillingDate());
        subscription.setEndDate(request.endDate());
        subscription.setReminderLeadDays(resolveReminderLeadDays(request.reminderLeadDays()));
        subscription.setPaymentMethod(paymentMethod);
        subscription.setNotes(request.notes());
        subscription.refreshAnchorsFromNextBillingDate();
        subscription.setUpdatedAt(Instant.now());

        Subscription saved = subscriptionRepository.save(subscription);
        logActivity(ActivityAction.UPDATE, saved, "Subscription updated");
        return toResponse(saved);
    }

    @Transactional
    public SubscriptionResponse pause(Long id) {
        tenantPermissionService.requireEditor();
        Long tenantId = TenantContext.getRequiredTenantId();
        Subscription subscription = loadSubscription(id, tenantId);

        if (subscription.getStatus() == SubscriptionLifecycleStatus.STOPPED
                || subscription.getStatus() == SubscriptionLifecycleStatus.ENDED) {
            throw new BadRequestException("Only active subscriptions can be paused");
        }
        if (subscription.getStatus() == SubscriptionLifecycleStatus.PAUSED) {
            return toResponse(subscription);
        }

        subscription.setStatus(SubscriptionLifecycleStatus.PAUSED);
        subscription.setUpdatedAt(Instant.now());
        Subscription saved = subscriptionRepository.save(subscription);
        logActivity(ActivityAction.STATUS_CHANGED, saved, "Subscription paused");
        return toResponse(saved);
    }

    @Transactional
    public SubscriptionResponse resume(Long id, LocalDate nextBillingDate) {
        tenantPermissionService.requireEditor();
        Long tenantId = TenantContext.getRequiredTenantId();
        Subscription subscription = loadSubscription(id, tenantId);

        if (subscription.getStatus() == SubscriptionLifecycleStatus.STOPPED) {
            throw new BadRequestException("Stopped subscriptions cannot be resumed");
        }
        if (subscription.getStatus() == SubscriptionLifecycleStatus.ENDED) {
            throw new BadRequestException("Ended subscriptions cannot be resumed");
        }
        if (nextBillingDate == null) {
            throw new BadRequestException("nextBillingDate is required when resuming");
        }
        if (subscription.getEndDate() != null && nextBillingDate.isAfter(subscription.getEndDate())) {
            throw new BadRequestException("nextBillingDate must be before or on endDate");
        }

        subscription.setNextBillingDate(nextBillingDate);
        subscription.refreshAnchorsFromNextBillingDate();
        subscription.setStatus(SubscriptionLifecycleStatus.ACTIVE);
        subscription.setUpdatedAt(Instant.now());

        Subscription saved = subscriptionRepository.save(subscription);
        logActivity(ActivityAction.STATUS_CHANGED, saved, "Subscription resumed");
        return toResponse(saved);
    }

    @Transactional
    public SubscriptionResponse stop(Long id) {
        tenantPermissionService.requireEditor();
        Long tenantId = TenantContext.getRequiredTenantId();
        Subscription subscription = loadSubscription(id, tenantId);

        if (subscription.getStatus() == SubscriptionLifecycleStatus.STOPPED) {
            return toResponse(subscription);
        }

        subscription.setStatus(SubscriptionLifecycleStatus.STOPPED);
        subscription.setUpdatedAt(Instant.now());
        Subscription saved = subscriptionRepository.save(subscription);
        logActivity(ActivityAction.STATUS_CHANGED, saved, "Subscription stopped");
        return toResponse(saved);
    }


    private Integer resolveReminderLeadDays(Integer requestedLeadDays) {
        int leadDays = requestedLeadDays != null ? requestedLeadDays : Subscription.DEFAULT_REMINDER_LEAD_DAYS;
        if (leadDays < 0 || leadDays > 90) {
            throw new BadRequestException("reminderLeadDays must be between 0 and 90");
        }
        return leadDays;
    }

    private void validateRequest(SubscriptionCreateRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.nextBillingDate())) {
            throw new BadRequestException("endDate must be on or after nextBillingDate");
        }
        if (request.frequency() == SubscriptionFrequency.CUSTOM
                && (request.customIntervalDays() == null || request.customIntervalDays() <= 0)) {
            throw new BadRequestException("customIntervalDays is required and must be positive for CUSTOM frequency");
        }
        if (request.frequency() != SubscriptionFrequency.CUSTOM && request.customIntervalDays() != null) {
            throw new BadRequestException("customIntervalDays is only allowed with CUSTOM frequency");
        }
    }

    private Subscription loadSubscription(Long id, Long tenantId) {
        return subscriptionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
    }

    private Category loadCategory(Long categoryId, Long tenantId) {
        return categoryRepository.findByIdAndTenantId(categoryId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return search.trim();
    }

    private List<Long> normalizeCategoryIds(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return null;
        }
        return categoryIds;
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

    private SubscriptionResponse toResponse(Subscription subscription) {
        LocalDate nextBillingDate = subscription.getNextBillingDate();
        LocalDate today = LocalDate.now();
        boolean overdue = nextBillingDate != null && nextBillingDate.isBefore(today);
        Long daysLeftBeforeDueDate = nextBillingDate == null
                ? null
                : Math.max(0L, ChronoUnit.DAYS.between(today, nextBillingDate));

        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getName(),
                subscription.getDescription(),
                subscription.getCategory().getId(),
                subscription.getCategory().getName(),
                subscription.getOwnerUserId(),
                subscription.getAmount(),
                subscription.getCurrency(),
                subscription.getFrequency(),
                subscription.getCustomIntervalDays(),
                nextBillingDate,
                overdue,
                daysLeftBeforeDueDate,
                subscription.getEndDate(),
                subscription.getStatus(),
                subscription.getReminderLeadDays(),
                subscription.getPaymentMethod() != null ? subscription.getPaymentMethod().getId() : null,
                subscription.getPaymentMethod() != null ? subscription.getPaymentMethod().getName() : null,
                subscription.getNotes(),
                subscription.getTenantId(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt(),
                subscription.getVersion()
        );
    }

    private void logActivity(ActivityAction action, Subscription subscription, String message) {
        activityLogService.record(
                action,
                ActivitySubjectType.SUBSCRIPTION,
                subscription.getId(),
                subscription.getName(),
                message,
                buildActivityMetadata(subscription)
        );
    }

    private String buildActivityMetadata(Subscription subscription) {
        return "{" +
                "\"id\":" + subscription.getId() + "," +
                "\"categoryId\":" + subscription.getCategory().getId() + "," +
                "\"amount\":" + subscription.getAmount() + "," +
                "\"status\":\"" + subscription.getStatus() + "\"," +
                "\"nextBillingDate\":\"" + subscription.getNextBillingDate() + "\"" +
                "}";
    }
}



