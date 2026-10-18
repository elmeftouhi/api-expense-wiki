package com.elmeftouhi.expense.activity.entity;

import com.elmeftouhi.expense.activity.ActivityAction;
import com.elmeftouhi.expense.activity.ActivitySubjectType;
import com.elmeftouhi.expense.multitenancy.BaseTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "activity_logs",
        indexes = {
                @Index(name = "idx_activity_logs_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_activity_logs_subject", columnList = "subject_type,subject_id"),
                @Index(name = "idx_activity_logs_occurred_at", columnList = "occurred_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ActivityLog extends BaseTenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 40)
    private ActivitySubjectType subjectType;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "subject_label", length = 160)
    private String subjectLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ActivityAction action;

    @Column(name = "actor_username", nullable = false, length = 120)
    private String actorUsername;

    @Column(length = 500)
    private String message;

    @Column(name = "metadata", length = 4000)
    private String metadata;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt = Instant.now();
}

