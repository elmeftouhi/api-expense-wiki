package com.elmeftouhi.expense.notification.controller;

import com.elmeftouhi.expense.notification.service.NotificationService;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.notification.NotificationStatus;
import com.elmeftouhi.expense.notification.dto.NotificationPageResponse;
import com.elmeftouhi.expense.notification.dto.NotificationResponse;
import com.elmeftouhi.expense.security.JwtPrincipal;
import com.elmeftouhi.expense.shared.exception.BadRequestException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> getNotifications() {
        Long tenantId = TenantContext.getRequiredTenantId();
        Long userId = getCurrentUserId();
        return notificationService.getUserNotifications(tenantId, userId);
    }

    @GetMapping("/page")
    public NotificationPageResponse getNotificationsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) NotificationStatus status) {
        if (page < 0) {
            throw new BadRequestException("page must be greater than or equal to 0");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }

        Long tenantId = TenantContext.getRequiredTenantId();
        Long userId = getCurrentUserId();
        return notificationService.getUserNotificationsPage(tenantId, userId, page, size, status);
    }

    @GetMapping("/unread")
    public List<NotificationResponse> getUnreadNotifications() {
        Long tenantId = TenantContext.getRequiredTenantId();
        Long userId = getCurrentUserId();
        return notificationService.getUnreadNotifications(tenantId, userId);
    }

    @GetMapping("/unread/count")
    public Long getUnreadCount() {
        Long tenantId = TenantContext.getRequiredTenantId();
        Long userId = getCurrentUserId();
        return notificationService.getUnreadCount(tenantId, userId);
    }

    @PatchMapping("/{id}/mark-as-read")
    public NotificationResponse markAsRead(@PathVariable Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Long userId = getCurrentUserId();
        return notificationService.markAsRead(id, tenantId, userId);
    }

    @PatchMapping("/{id}/mark-as-archived")
    public NotificationResponse markAsArchived(@PathVariable Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Long userId = getCurrentUserId();
        return notificationService.markAsArchived(id, tenantId, userId);
    }

    @PostMapping("/mark-all-as-read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead() {
        Long tenantId = TenantContext.getRequiredTenantId();
        Long userId = getCurrentUserId();
        notificationService.markAllAsRead(tenantId, userId);
    }

    private Long getCurrentUserId() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (authentication == null) {
            throw new BadRequestException("Authenticated user not found");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtPrincipal jwtPrincipal) {
            return jwtPrincipal.userId();
        }

        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Authenticated user id is invalid");
        }
    }
}
