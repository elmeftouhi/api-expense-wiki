package com.elmeftouhi.expense.expense;

import com.elmeftouhi.expense.multitenancy.BaseTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "expenses",
        indexes = {
                @Index(name = "idx_expenses_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_expenses_category_id", columnList = "category_id"),
                @Index(name = "idx_expenses_user_id", columnList = "user_id"),
                @Index(name = "idx_expenses_status", columnList = "status"),
                @Index(name = "idx_expenses_due_date", columnList = "due_date"),
                @Index(name = "idx_expenses_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_expenses_tenant_category", columnList = "tenant_id, category_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Expense extends BaseTenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private com.elmeftouhi.expense.category.Category category;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 10, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ExpenseType type = ExpenseType.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExpenseStatus status = ExpenseStatus.PENDING;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "payment_method", length = 100)
    private String paymentMethod;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;

    @OneToOne(mappedBy = "expense", fetch = FetchType.LAZY)
    private ExpenseSubscription subscription;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public void markAsDeleted() {
        this.deleted = true;
        this.deletedAt = Instant.now();
    }

    public void markAsPaid(LocalDate paidDate) {
        this.paidDate = paidDate;
        this.status = ExpenseStatus.PAID;
        this.updatedAt = Instant.now();
    }
}
