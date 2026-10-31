package com.elmeftouhi.expense.notification.service;

import com.elmeftouhi.expense.notification.NotificationType;
import com.elmeftouhi.expense.notification.NotificationStatus;
import com.elmeftouhi.expense.notification.entity.Notification;
import com.elmeftouhi.expense.notification.repository.NotificationRepository;
import com.elmeftouhi.expense.expense.entity.Expense;
import com.elmeftouhi.expense.expense.entity.Subscription;
import com.elmeftouhi.expense.notification.dto.NotificationPageResponse;
import com.elmeftouhi.expense.notification.dto.NotificationResponse;
import com.elmeftouhi.expense.shared.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRealtimeService notificationRealtimeService;

    @Transactional
    public void sendNotification(
            Long tenantId,
            Long userId,
            NotificationType type,
            String title,
            String message,
            String subjectType,
            Long subjectId) {
        Notification notification = new Notification();
        notification.assignTenantId(tenantId);
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setSubjectType(subjectType);
        notification.setSubjectId(subjectId);
        notification.setStatus(NotificationStatus.SENT);

        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = toResponse(saved);
        runAfterCommit(() -> notificationRealtimeService.publishCreated(tenantId, userId, response));
    }

    @Transactional
    public void sendExpenseNotification(
            Expense expense,
            NotificationType type,
            String message) {
        sendNotification(
                expense.getTenantId(),
                expense.getUserId(),
                type,
                type.getDescription(),
                message,
                "EXPENSE",
                expense.getId()
        );
    }

    @Transactional
    public void sendSubscriptionNotification(
            Subscription subscription,
            NotificationType type,
            String message) {
        sendNotification(
                subscription.getTenantId(),
                subscription.getOwnerUserId(),
                type,
                type.getDescription(),
                message,
                "SUBSCRIPTION",
                subscription.getId()
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long tenantId, Long userId) {
        return notificationRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(Long tenantId, Long userId) {
        return notificationRepository.findByTenantIdAndUserIdAndStatusOrderByCreatedAtDesc(
                tenantId, userId, NotificationStatus.SENT)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long tenantId, Long userId) {
        return notificationRepository.countByTenantIdAndUserIdAndStatus(tenantId, userId, NotificationStatus.SENT);
    }

    @Transactional
    public NotificationResponse markAsRead(Long id, Long tenantId, Long userId) {
        Notification notification = notificationRepository.findByIdAndTenantIdAndUserId(id, tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        notification.markAsRead();
        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = toResponse(saved);
        runAfterCommit(() -> notificationRealtimeService.publishUpdated(tenantId, userId, response));

        return response;
    }

    @Transactional
    public NotificationResponse markAsArchived(Long id, Long tenantId, Long userId) {
        Notification notification = notificationRepository.findByIdAndTenantIdAndUserId(id, tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        notification.markAsArchived();
        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = toResponse(saved);
        runAfterCommit(() -> notificationRealtimeService.publishUpdated(tenantId, userId, response));

        return response;
    }

    @Transactional
    public void markAllAsRead(Long tenantId, Long userId) {
        List<Notification> unread = notificationRepository.findByTenantIdAndUserIdAndStatusOrderByCreatedAtDesc(
                tenantId, userId, NotificationStatus.SENT);

        unread.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unread);
        int changedCount = unread.size();
        if (changedCount > 0) {
            runAfterCommit(() -> notificationRealtimeService.publishBulkMarkedAsRead(tenantId, userId, changedCount));
        }
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse getUserNotificationsPage(
            Long tenantId,
            Long userId,
            int page,
            int size,
            NotificationStatus status) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<Notification> resultPage = status == null
                ? notificationRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId, pageRequest)
                : notificationRepository.findByTenantIdAndUserIdAndStatusOrderByCreatedAtDesc(
                        tenantId,
                        userId,
                        status,
                        pageRequest);

        return new NotificationPageResponse(
                resultPage.map(this::toResponse).getContent(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages(),
                resultPage.hasNext()
        );
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getStatus(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getSubjectType(),
                notification.getSubjectId(),
                notification.getData(),
                notification.getTenantId(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                notification.getArchivedAt()
        );
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }
}
