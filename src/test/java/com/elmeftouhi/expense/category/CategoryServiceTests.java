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
        categoryRepository.findAll().stream()
                .filter(category -> category.getParentCategory() != null)
                .forEach(categoryRepository::delete);
        categoryRepository.findAll().stream()
                .filter(category -> category.getParentCategory() == null)
                .forEach(categoryRepository::delete);
        activityLogRepository.deleteAll();
    }

    @Test
    void categoriesAreScopedToCurrentTenant() {
        TenantContext.setTenantId(1L);
        CategoryResponse created = categoryService.create(new CategoryCreateRequest(
                null,
                "Food",
                "Daily meals",
                "utensils",
                "#111111",
                "#ffffff",
                new BigDecimal("250.00"),
                1,
                true,
                false
        ));
        assertEquals(1L, created.tenantId());
        assertEquals("Food", created.name());
        assertEquals(1, created.level());
        assertThat(created.subCategories()).isEmpty();

        CategoryResponse createdTransport = categoryService.create(new CategoryCreateRequest(
                null,
                "Transport",
                null,
                null,
                null,
                null,
                new BigDecimal("100.00"),
                2,
                true,
                true
        ));
        assertEquals(1L, createdTransport.tenantId());

        List<CategoryResponse> tenantOneCategories = categoryService.findAll();
        assertThat(tenantOneCategories).hasSize(2);
        assertEquals("Food", tenantOneCategories.get(0).name());
        assertEquals("Transport", tenantOneCategories.get(1).name());

        TenantContext.setTenantId(2L);
        assertThat(categoryService.findAll()).isEmpty();
    }

    @Test
    void cannotReuseCategoryNameWithinSameTenant() {
        TenantContext.setTenantId(1L);
        CategoryCreateRequest request = new CategoryCreateRequest(
                null,
                "Food",
                null,
                null,
                null,
                null,
                null,
                1,
                true,
                false
        );

        categoryService.create(request);

        ConflictException exception = assertThrows(ConflictException.class, () -> categoryService.create(
                new CategoryCreateRequest(null, " food ", null, null, null, null, null, 1, true, false)
        ));

        assertThat(exception.getMessage()).contains("already exists");
    }

    @Test
    void categoriesCannotBeReadAcrossTenants() {
        TenantContext.setTenantId(1L);
        CategoryResponse created = categoryService.create(new CategoryCreateRequest(
                null,
                "Health",
                null,
                null,
                null,
                null,
                null,
                1,
                true,
                false
        ));

        TenantContext.setTenantId(2L);
        assertThrows(ResourceNotFoundException.class, () -> categoryService.getById(created.id()));
    }

    @Test
    void subcategoriesAreReturnedWithTheirParentCategory() {
        TenantContext.setTenantId(1L);
        CategoryResponse parent = categoryService.create(new CategoryCreateRequest(
                null,
                "Food",
                "Meals at home",
                null,
                null,
                null,
                null,
                1,
                true,
                false
        ));

        CategoryResponse subCategory = categoryService.create(new CategoryCreateRequest(
                parent.id(),
                "Groceries",
                "Supermarket expenses",
                null,
                null,
                null,
                null,
                2,
                true,
                false
        ));

        assertEquals(parent.id(), subCategory.parentCategoryId());
        assertThat(subCategory.subCategories()).isEmpty();

        CategoryResponse loadedParent = categoryService.getById(parent.id());
        assertThat(loadedParent.subCategories())
                .hasSize(1)
                .first()
                .extracting(CategoryResponse::name)
                .isEqualTo("Groceries");

        assertThat(categoryService.findAll())
                .hasSize(1)
                .first()
                .extracting(CategoryResponse::subCategories)
                .satisfies(subCategories -> assertThat(subCategories).hasSize(1));
    }

    @Test
    void inactiveCategoriesAreHiddenUnlessRequested() {
        TenantContext.setTenantId(1L);
        categoryService.create(new CategoryCreateRequest(
                null,
                "Active A",
                null,
                null,
                null,
                null,
                null,
                2,
                true,
                false
        ));
        categoryService.create(new CategoryCreateRequest(
                null,
                "Inactive B",
                null,
                null,
                null,
                null,
                null,
                1,
                false,
                false
        ));
        categoryService.create(new CategoryCreateRequest(
                null,
                "Active C",
                null,
                null,
                null,
                null,
                null,
                1,
                true,
                false
        ));

        assertThat(categoryService.findAll())
                .extracting(CategoryResponse::name)
                .containsExactly("Active C", "Active A");

        assertThat(categoryService.findAll(true))
                .extracting(CategoryResponse::name)
                .containsExactly("Active C", "Inactive B", "Active A");
    }

    @Test
    void parentCategoryMustBelongToTheCurrentTenant() {
        TenantContext.setTenantId(1L);
        CategoryResponse parent = categoryService.create(new CategoryCreateRequest(
                null,
                "Health",
                null,
                null,
                null,
                null,
                null,
                1,
                true,
                false
        ));

        TenantContext.setTenantId(2L);
        assertThrows(ResourceNotFoundException.class, () -> categoryService.create(new CategoryCreateRequest(
                parent.id(),
                "Pharmacy",
                null,
                null,
                null,
                null,
                null,
                1,
                true,
                false
        )));
    }

    @Test
    void updateAndDeleteCategoryWithinTenant() {
        TenantContext.setTenantId(1L);
        CategoryResponse created = categoryService.create(new CategoryCreateRequest(
                null,
                "Subscriptions",
                "Monthly services",
                "credit-card",
                "#222222",
                "#ffffff",
                new BigDecimal("49.99"),
                3,
                true,
                false
        ));

        CategoryResponse updated = categoryService.update(created.id(), new CategoryCreateRequest(
                null,
                "Subscriptions & Apps",
                "Monthly software subscriptions",
                "app-store",
                "#333333",
                "#fafafa",
                new BigDecimal("79.99"),
                4,
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

    @Test
    void cannotDeleteCategoryThatHasSubcategories() {
        TenantContext.setTenantId(1L);
        CategoryResponse parent = categoryService.create(new CategoryCreateRequest(
                null,
                "Travel",
                null,
                null,
                null,
                null,
                null,
                1,
                true,
                false
        ));

        categoryService.create(new CategoryCreateRequest(
                parent.id(),
                "Flights",
                null,
                null,
                null,
                null,
                null,
                2,
                true,
                false
        ));

        assertThrows(ConflictException.class, () -> categoryService.delete(parent.id()));
    }
}

