package com.elmeftouhi.expense.category.entity;

import com.elmeftouhi.expense.multitenancy.BaseTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "categories",
        indexes = {
                @Index(name = "idx_categories_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_categories_tenant_name", columnList = "tenant_id, name"),
                @Index(name = "idx_categories_tenant_parent", columnList = "tenant_id, parent_category_id"),
                @Index(name = "idx_categories_tenant_level", columnList = "tenant_id, level")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Category extends BaseTenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private Category parentCategory;

    @OneToMany(mappedBy = "parentCategory")
    @OrderBy("level ASC, name ASC")
    private List<Category> subCategories = new ArrayList<>();

    @Column(name = "level")
    private Integer level = 0;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 100)
    private String icon;

    @Column(name = "text_color", length = 20)
    private String textColor;

    @Column(name = "background_color", length = 20)
    private String backgroundColor;

    @Column(name = "monthly_budget", precision = 19, scale = 2)
    private BigDecimal monthlyBudget;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "allow_over_budget", nullable = false)
    private boolean allowOverBudget = false;
}

