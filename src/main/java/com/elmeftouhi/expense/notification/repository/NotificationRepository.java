package com.elmeftouhi.expense.notification.repository;

import com.elmeftouhi.expense.notification.NotificationStatus;
import com.elmeftouhi.expense.notification.entity.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByTenantIdAndUserIdOrderByCreatedAtDesc(Long tenantId, Long userId);

    List<Notification> findByTenantIdAndUserIdAndStatusOrderByCreatedAtDesc(
            Long tenantId, Long userId, NotificationStatus status);

    @Query("""
            SELECT n FROM Notification n
            WHERE n.tenantId = :tenantId
                AND n.userId = :userId
                AND n.status = :status
            ORDER BY n.createdAt DESC
            LIMIT :limit
            """)
    List<Notification> findLatest(
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId,
            @Param("status") NotificationStatus status,
            @Param("limit") int limit);

    long countByTenantIdAndUserIdAndStatus(Long tenantId, Long userId, NotificationStatus status);

    Optional<Notification> findByIdAndTenantId(Long id, Long tenantId);
}

