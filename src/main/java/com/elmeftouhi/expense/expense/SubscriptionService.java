package com.elmeftouhi.expense.expense;

import com.elmeftouhi.expense.activity.ActivityAction;
import com.elmeftouhi.expense.activity.ActivityLogService;
import com.elmeftouhi.expense.activity.ActivitySubjectType;
import com.elmeftouhi.expense.category.Category;
import com.elmeftouhi.expense.category.CategoryRepository;
import com.elmeftouhi.expense.expense.dto.SubscriptionCreateRequest;
import com.elmeftouhi.expense.expense.dto.SubscriptionOccurrenceResponse;
import com.elmeftouhi.expense.expense.dto.SubscriptionResponse;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.security.JwtPrincipal;
import com.elmeftouhi.expense.shared.exception.BadRequestException;
import com.elmeftouhi.expense.shared.exception.ResourceNotFoundException;
import com.elmeftouhi.expense.tenantconfig.ConfigurationContext;
import java.time.Instant;
import java.time.LocalDate;
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
    private final SubscriptionOccurrenceRepository occurrenceRepository;
    private final CategoryRepository categoryRepository;
    private final ActivityLogService activityLogService;

    @Transactional
    public SubscriptionResponse create(SubscriptionCreateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Long currentUserId = getCurrentUserId();

        Category category = loadCategory(request.categoryId(), tenantId);
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
        subscription.setPaymentMethod(request.paymentMethod());
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
            Long categoryId,
            SubscriptionFrequency frequency,
            LocalDate nextBillingFrom,
            LocalDate nextBillingTo,
            Pageable pageable) {
        Long tenantId = TenantContext.getRequiredTenantId();

        if (nextBillingFrom != null && nextBillingTo != null && nextBillingTo.isBefore(nextBillingFrom)) {
            throw new BadRequestException("nextBillingTo must be greater than or equal to nextBillingFrom");
        }

        String normalizedSearch = normalizeSearch(search);
        if (normalizedSearch == null) {
            return subscriptionRepository.searchWithoutText(
                            tenantId,
                            status,
                            categoryId,
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
                        categoryId,
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
        Long tenantId = TenantContext.getRequiredTenantId();
        Subscription subscription = loadSubscription(id, tenantId);

        if (subscription.getStatus() == SubscriptionLifecycleStatus.STOPPED
                || subscription.getStatus() == SubscriptionLifecycleStatus.ENDED) {
            throw new BadRequestException("Stopped or ended subscriptions cannot be edited");
        }

        Category category = loadCategory(request.categoryId(), tenantId);
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
        subscription.setPaymentMethod(request.paymentMethod());
        subscription.setNotes(request.notes());
        subscription.refreshAnchorsFromNextBillingDate();
        subscription.setUpdatedAt(Instant.now());

        Subscription saved = subscriptionRepository.save(subscription);
        logActivity(ActivityAction.UPDATE, saved, "Subscription updated");
        return toResponse(saved);
    }

    @Transactional
    public SubscriptionResponse pause(Long id) {
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

    @Transactional(readOnly = true)
    public Page<SubscriptionOccurrenceResponse> getOccurrenceHistory(Long subscriptionId, Pageable pageable) {
        Long tenantId = TenantContext.getRequiredTenantId();
        loadSubscription(subscriptionId, tenantId);

        return occurrenceRepository
                .findBySubscriptionIdAndTenantIdOrderByScheduledBillingDateDesc(subscriptionId, tenantId, pageable)
                .map(occurrence -> new SubscriptionOccurrenceResponse(
                        occurrence.getId(),
                        occurrence.getSubscription().getId(),
                        occurrence.getScheduledBillingDate(),
                        occurrence.getGeneratedExpense().getId(),
                        occurrence.getGeneratedExpense().getAmount(),
                        occurrence.getGeneratedExpense().getCurrency(),
                        occurrence.getGeneratedExpense().getStatus(),
                        occurrence.getGeneratedExpense().getDueDate(),
                        occurrence.getGeneratedExpense().getPaidDate(),
                        occurrence.getCreatedAt()
                ));
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
                subscription.getNextBillingDate(),
                subscription.getEndDate(),
                subscription.getStatus(),
                subscription.getReminderLeadDays(),
                subscription.getPaymentMethod(),
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



