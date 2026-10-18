package com.elmeftouhi.expense.notification;

import com.elmeftouhi.expense.notification.entity.Notification;
import com.elmeftouhi.expense.notification.repository.NotificationRepository;
import com.elmeftouhi.expense.notification.service.NotificationService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.elmeftouhi.expense.notification.dto.NotificationResponse;
import com.elmeftouhi.expense.shared.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Tests")
class NotificationServiceTests {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Long tenantId = 1L;
    private Long userId = 1L;

    @Test
    @DisplayName("Should send a notification")
    void testSendNotification() {
        // Given
        Notification notification = new Notification();
        notification.setId(1L);
        notification.assignTenantId(tenantId);
        notification.setUserId(userId);
        notification.setType(NotificationType.EXPENSE_CREATED);
        notification.setTitle("New Expense");
        notification.setMessage("A new expense has been created");
        notification.setStatus(NotificationStatus.SENT);

        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // When
        notificationService.sendNotification(
                tenantId,
                userId,
                NotificationType.EXPENSE_CREATED,
                "New Expense",
                "A new expense has been created",
                "EXPENSE",
                1L
        );

        // Then
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should get user notifications")
    void testGetUserNotifications() {
        // Given
        Notification notification1 = new Notification();
        notification1.setId(1L);
        notification1.setUserId(userId);
        notification1.setType(NotificationType.EXPENSE_CREATED);
        notification1.setStatus(NotificationStatus.SENT);

        Notification notification2 = new Notification();
        notification2.setId(2L);
        notification2.setUserId(userId);
        notification2.setType(NotificationType.EXPENSE_OVERDUE);
        notification2.setStatus(NotificationStatus.SENT);

        when(notificationRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId))
                .thenReturn(List.of(notification1, notification2));

        // When
        List<NotificationResponse> results = notificationService.getUserNotifications(tenantId, userId);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(notificationRepository, times(1)).findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId);
    }

    @Test
    @DisplayName("Should get unread notifications")
    void testGetUnreadNotifications() {
        // Given
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUserId(userId);
        notification.setType(NotificationType.EXPENSE_CREATED);
        notification.setStatus(NotificationStatus.SENT);

        when(notificationRepository.findByTenantIdAndUserIdAndStatusOrderByCreatedAtDesc(
                tenantId, userId, NotificationStatus.SENT))
                .thenReturn(List.of(notification));

        // When
        List<NotificationResponse> results = notificationService.getUnreadNotifications(tenantId, userId);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(NotificationStatus.SENT, results.get(0).status());
    }

    @Test
    @DisplayName("Should get unread count")
    void testGetUnreadCount() {
        // Given
        when(notificationRepository.countByTenantIdAndUserIdAndStatus(tenantId, userId, NotificationStatus.SENT))
                .thenReturn(5L);

        // When
        long count = notificationService.getUnreadCount(tenantId, userId);

        // Then
        assertEquals(5L, count);
    }

    @Test
    @DisplayName("Should mark notification as read")
    void testMarkAsRead() {
        // Given
        Notification notification = new Notification();
        notification.setId(1L);
        notification.assignTenantId(tenantId);
        notification.setStatus(NotificationStatus.SENT);
        notification.setType(NotificationType.EXPENSE_CREATED);

        when(notificationRepository.findByIdAndTenantId(1L, tenantId))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // When
        NotificationResponse response = notificationService.markAsRead(1L, tenantId);

        // Then
        assertNotNull(response);
        assertEquals(NotificationStatus.READ, response.status());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should mark notification as archived")
    void testMarkAsArchived() {
        // Given
        Notification notification = new Notification();
        notification.setId(1L);
        notification.assignTenantId(tenantId);
        notification.setStatus(NotificationStatus.SENT);

        when(notificationRepository.findByIdAndTenantId(1L, tenantId))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // When
        NotificationResponse response = notificationService.markAsArchived(1L, tenantId);

        // Then
        assertNotNull(response);
        assertEquals(NotificationStatus.ARCHIVED, response.status());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should throw exception when marking non-existent notification")
    void testMarkNonExistentNotification() {
        // Given
        when(notificationRepository.findByIdAndTenantId(999L, tenantId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAsRead(999L, tenantId));
    }

    @Test
    @DisplayName("Should mark all notifications as read")
    void testMarkAllAsRead() {
        // Given
        Notification notification1 = new Notification();
        notification1.setId(1L);
        notification1.setStatus(NotificationStatus.SENT);

        Notification notification2 = new Notification();
        notification2.setId(2L);
        notification2.setStatus(NotificationStatus.SENT);

        when(notificationRepository.findByTenantIdAndUserIdAndStatusOrderByCreatedAtDesc(
                tenantId, userId, NotificationStatus.SENT))
                .thenReturn(List.of(notification1, notification2));

        // When
        notificationService.markAllAsRead(tenantId, userId);

        // Then
        verify(notificationRepository, times(1)).saveAll(any());
    }
}

