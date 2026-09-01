package com.elmeftouhi.expense.notification;

import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.notification.dto.NotificationResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> getNotifications() {
        Long tenantId = TenantContext.getRequiredTenantId();
        Long userId = getCurrentUserId();
        return notificationService.getUserNotifications(tenantId, userId);
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
        return notificationService.markAsRead(id, tenantId);
    }

    @PatchMapping("/{id}/mark-as-archived")
    public NotificationResponse markAsArchived(@PathVariable Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        return notificationService.markAsArchived(id, tenantId);
    }

    @PostMapping("/mark-all-as-read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead() {
        Long tenantId = TenantContext.getRequiredTenantId();
        Long userId = getCurrentUserId();
        notificationService.markAllAsRead(tenantId, userId);
    }

    private Long getCurrentUserId() {
        return Long.parseLong(org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName());
    }
}

