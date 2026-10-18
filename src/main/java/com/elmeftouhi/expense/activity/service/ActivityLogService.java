package com.elmeftouhi.expense.activity.service;

import com.elmeftouhi.expense.activity.ActivityAction;
import com.elmeftouhi.expense.activity.ActivitySubjectType;
import com.elmeftouhi.expense.activity.entity.ActivityLog;
import com.elmeftouhi.expense.activity.repository.ActivityLogRepository;
import com.elmeftouhi.expense.activity.dto.ActivityLogPageResponse;
import com.elmeftouhi.expense.activity.dto.ActivityLogResponse;
import com.elmeftouhi.expense.activity.dto.ActivityActorResponse;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.security.JwtPrincipal;
import com.elmeftouhi.expense.shared.exception.BadRequestException;
import com.elmeftouhi.expense.user.entity.AppUser;
import com.elmeftouhi.expense.user.repository.AppUserRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final AppUserRepository appUserRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(ActivityAction action,
                       ActivitySubjectType subjectType,
                       Long subjectId,
                       String subjectLabel,
                       String message,
                       String metadata) {
        ActivityLog entry = new ActivityLog();
        entry.setAction(action);
        entry.setSubjectType(subjectType);
        entry.setSubjectId(subjectId);
        entry.setSubjectLabel(normalize(subjectLabel));
        entry.setActorUsername(resolveCurrentUsername());
        entry.setMessage(normalize(message));
        entry.setMetadata(normalize(metadata));
        activityLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public ActivityLogPageResponse search(
            ActivityAction action,
            ActivitySubjectType subjectType,
            Long subjectId,
            Instant from,
            Instant to,
            String actor,
            int page,
            int size) {

        if (page < 0) {
            throw new BadRequestException("Page must be >= 0");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Size must be between 1 and 100");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("from must be before or equal to to");
        }

        Long tenantId = TenantContext.getRequiredTenantId();

        Specification<ActivityLog> spec = byTenant(tenantId);

        if (action != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action));
        }
        if (subjectType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("subjectType"), subjectType));
        }
        if (subjectId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("subjectId"), subjectId));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), to));
        }
        if (actor != null && !actor.isBlank()) {
            String normalizedActor = actor.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("actorUsername")), normalizedActor));
        }

        Pageable pageable = PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.desc("occurredAt"),
                        org.springframework.data.domain.Sort.Order.desc("id")));

        Page<ActivityLog> result = activityLogRepository.findAll(spec, pageable);
        Map<String, ActivityActorResponse> actorCache = new HashMap<>();

        return new ActivityLogPageResponse(
                result.getContent().stream().map(entry -> toResponse(entry, actorCache)).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext());
    }

    private Specification<ActivityLog> byTenant(Long tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    private ActivityLogResponse toResponse(
            ActivityLog entry,
            Map<String, ActivityActorResponse> actorCache) {
        String actorKey = normalizeActorKey(entry.getActorUsername());
        return new ActivityLogResponse(
                entry.getId(),
                entry.getSubjectType(),
                entry.getSubjectId(),
                entry.getSubjectLabel(),
                entry.getAction(),
                entry.getActorUsername(),
                resolveActorUser(actorKey, actorCache),
                entry.getMessage(),
                entry.getMetadata(),
                entry.getOccurredAt());
    }

    private ActivityActorResponse resolveActorUser(
            String actorKey,
            Map<String, ActivityActorResponse> actorCache) {
        if (actorKey == null || "system".equals(actorKey)) {
            return null;
        }
        if (actorCache.containsKey(actorKey)) {
            return actorCache.get(actorKey);
        }

        ActivityActorResponse actor = appUserRepository
                .findByEmail(actorKey)
                .map(this::toActorResponse)
                .orElse(null);

        actorCache.put(actorKey, actor);
        return actor;
    }

    private ActivityActorResponse toActorResponse(AppUser user) {
        return new ActivityActorResponse(
                user.getId(),
                user.getEmail(),
                buildDisplayedName(user),
                user.getFirstName(),
                user.getLastName());
    }

    private String buildDisplayedName(AppUser user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();

        if (firstName != null && !firstName.isBlank() && lastName != null && !lastName.isBlank()) {
            return firstName + " " + lastName;
        }
        if (firstName != null && !firstName.isBlank()) {
            return firstName;
        }
        if (lastName != null && !lastName.isBlank()) {
            return lastName;
        }
        return user.getEmail();
    }

    private String normalizeActorKey(String actorUsername) {
        if (actorUsername == null) {
            return null;
        }
        String normalized = actorUsername.trim().toLowerCase();
        return normalized.isEmpty() ? null : normalized;
    }

    private String resolveCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "system";
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof JwtPrincipal jp && jp.username() != null && !jp.username().isBlank()) {
            return jp.username();
        }
        if (principal instanceof String name && !name.isBlank() && !"anonymousUser".equalsIgnoreCase(name)) {
            return name;
        }

        String authName = auth.getName();
        if (authName != null && !authName.isBlank() && !"anonymousUser".equalsIgnoreCase(authName)) {
            return authName;
        }

        return "system";
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}


