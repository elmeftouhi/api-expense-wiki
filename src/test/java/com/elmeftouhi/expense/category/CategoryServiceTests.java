package com.elmeftouhi.expense.category;

import static com.elmeftouhi.expense.activity.ActivityAction.CREATE;
import static com.elmeftouhi.expense.activity.ActivityAction.DELETE;
import static com.elmeftouhi.expense.activity.ActivityAction.UPDATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.elmeftouhi.expense.activity.ActivityLog;
import com.elmeftouhi.expense.activity.ActivityLogRepository;
import com.elmeftouhi.expense.activity.ActivitySubjectType;
import com.elmeftouhi.expense.category.dto.CategoryCreateRequest;
import com.elmeftouhi.expense.category.dto.CategoryResponse;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.shared.exception.ConflictException;
import com.elmeftouhi.expense.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CategoryServiceTests {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
        categoryRepository.deleteAll();
        activityLogRepository.deleteAll();
    }

    @Test
    void categoriesAreScopedToCurrentTenant() {
        TenantContext.setTenantId(1L);
        CategoryResponse created = categoryService.create(new CategoryCreateRequest(
                "Food",
                "Daily meals",
                "utensils",
                "#111111",
                "#ffffff",
                new BigDecimal("250.00"),
                true,
                false
        ));
        assertEquals(1L, created.tenantId());
        assertEquals("Food", created.name());

        CategoryResponse createdTransport = categoryService.create(new CategoryCreateRequest(
                "Transport",
                null,
                null,
                null,
                null,
                new BigDecimal("100.00"),
                true,
                true
        ));
        assertEquals(1L, createdTransport.tenantId());

        List<CategoryResponse> tenantOneCategories = categoryService.findAll();
        assertThat(tenantOneCategories).hasSize(2);

        TenantContext.setTenantId(2L);
        assertThat(categoryService.findAll()).isEmpty();
    }

    @Test
    void cannotReuseCategoryNameWithinSameTenant() {
        TenantContext.setTenantId(1L);
        CategoryCreateRequest request = new CategoryCreateRequest(
                "Food",
                null,
                null,
                null,
                null,
                null,
                true,
                false
        );

        categoryService.create(request);

        ConflictException exception = assertThrows(ConflictException.class, () -> categoryService.create(
                new CategoryCreateRequest(" food ", null, null, null, null, null, true, false)
        ));

        assertThat(exception.getMessage()).contains("already exists");
    }

    @Test
    void categoriesCannotBeReadAcrossTenants() {
        TenantContext.setTenantId(1L);
        CategoryResponse created = categoryService.create(new CategoryCreateRequest(
                "Health",
                null,
                null,
                null,
                null,
                null,
                true,
                false
        ));

        TenantContext.setTenantId(2L);
        assertThrows(ResourceNotFoundException.class, () -> categoryService.getById(created.id()));
    }

    @Test
    void updateAndDeleteCategoryWithinTenant() {
        TenantContext.setTenantId(1L);
        CategoryResponse created = categoryService.create(new CategoryCreateRequest(
                "Subscriptions",
                "Monthly services",
                "credit-card",
                "#222222",
                "#ffffff",
                new BigDecimal("49.99"),
                true,
                false
        ));

        CategoryResponse updated = categoryService.update(created.id(), new CategoryCreateRequest(
                "Subscriptions & Apps",
                "Monthly software subscriptions",
                "app-store",
                "#333333",
                "#fafafa",
                new BigDecimal("79.99"),
                false,
                true
        ));

        assertEquals(created.id(), updated.id());
        assertEquals("Subscriptions & Apps", updated.name());
        assertFalse(updated.active());
        assertTrue(updated.allowOverBudget());

        categoryService.delete(updated.id());
        assertThat(categoryService.findAll()).isEmpty();
        assertThrows(ResourceNotFoundException.class, () -> categoryService.getById(updated.id()));

        assertThat(activityLogRepository.findAll())
                .hasSize(3)
                .extracting(ActivityLog::getSubjectType)
                .containsOnly(ActivitySubjectType.CATEGORY);
        assertThat(activityLogRepository.findAll())
                .extracting(ActivityLog::getAction)
                .containsExactlyInAnyOrder(CREATE, UPDATE, DELETE);
        assertThat(activityLogRepository.findAll())
                .extracting(ActivityLog::getTenantId)
                .containsOnly(1L);
    }
}

