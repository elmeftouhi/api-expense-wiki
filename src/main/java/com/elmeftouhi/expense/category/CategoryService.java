package com.elmeftouhi.expense.category;

import com.elmeftouhi.expense.activity.ActivityAction;
import com.elmeftouhi.expense.activity.ActivityLogService;
import com.elmeftouhi.expense.activity.ActivitySubjectType;
import com.elmeftouhi.expense.category.dto.CategoryCreateRequest;
import com.elmeftouhi.expense.category.dto.CategoryResponse;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.shared.exception.BadRequestException;
import com.elmeftouhi.expense.shared.exception.ConflictException;
import com.elmeftouhi.expense.shared.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return findAll(false);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll(boolean includeInactive) {
        Long tenantId = TenantContext.getRequiredTenantId();
        return categoryRepository.findRootCategories(tenantId, includeInactive).stream()
                .map(category -> toResponse(category, includeInactive))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAllIncludingInactive(boolean includeInactive) {
        return findAll(includeInactive);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        return toResponse(loadCategory(id, tenantId), true);
    }

    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        String normalizedName = normalizeName(request.name());
        ensureNameIsAvailable(tenantId, normalizedName, null);

        Category category = new Category();
        category.setParentCategory(resolveParentCategory(tenantId, request.parentCategoryId(), null));
        applyRequest(category, request);
        category.setName(normalizedName);
        category.setLevel(request.level());

        CategoryResponse response = toResponse(categoryRepository.save(category), true);
        logActivity(ActivityAction.CREATE, response, buildActivityMessage("created"), buildActivityMetadata(response));
        return response;
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryCreateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Category category = loadCategory(id, tenantId);
        String normalizedName = normalizeName(request.name());
        ensureNameIsAvailable(tenantId, normalizedName, id);

        category.setParentCategory(resolveParentCategory(tenantId, request.parentCategoryId(), id));
        applyRequest(category, request);
        category.setName(normalizedName);
        category.setLevel(request.level());

        CategoryResponse response = toResponse(categoryRepository.save(category), true);
        logActivity(ActivityAction.UPDATE, response, buildActivityMessage("updated"), buildActivityMetadata(response));
        return response;
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Category category = loadCategory(id, tenantId);
        if (categoryRepository.existsByTenantIdAndParentCategoryId(tenantId, id)) {
            throw new ConflictException("Cannot delete a category that has subcategories");
        }
        CategoryResponse response = toResponse(category, true);
        categoryRepository.delete(category);
        logActivity(ActivityAction.DELETE, response, buildActivityMessage("deleted"), buildActivityMetadata(response));
    }

    private Category loadCategory(Long id, Long tenantId) {
        return categoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private void ensureNameIsAvailable(Long tenantId, String normalizedName, Long currentCategoryId) {
        categoryRepository.findByTenantIdAndNameIgnoreCase(tenantId, normalizedName)
                .filter(existing -> currentCategoryId == null || !currentCategoryId.equals(existing.getId()))
                .ifPresent(existing -> {
                    throw new ConflictException("Category name already exists");
                });
    }

    private Category resolveParentCategory(Long tenantId, Long parentCategoryId, Long currentCategoryId) {
        if (parentCategoryId == null) {
            return null;
        }

        Category parentCategory = loadCategory(parentCategoryId, tenantId);
        if (currentCategoryId != null && currentCategoryId.equals(parentCategory.getId())) {
            throw new BadRequestException("A category cannot be its own parent");
        }
        if (parentCategory.getParentCategory() != null) {
            throw new BadRequestException("Parent category must be a root category");
        }
        return parentCategory;
    }

    private void applyRequest(Category category, CategoryCreateRequest request) {
        category.setDescription(normalizeOptional(request.description()));
        category.setIcon(normalizeOptional(request.icon()));
        category.setTextColor(normalizeOptional(request.textColor()));
        category.setBackgroundColor(normalizeOptional(request.backgroundColor()));
        category.setMonthlyBudget(request.monthlyBudget());
        category.setActive(Boolean.TRUE.equals(request.active()));
        category.setAllowOverBudget(Boolean.TRUE.equals(request.allowOverBudget()));
    }

    private CategoryResponse toResponse(Category category, boolean includeInactive) {
        List<CategoryResponse> subCategories = categoryRepository
                .findChildCategories(category.getTenantId(), category.getId(), includeInactive)
                .stream()
                .map(this::toChildResponse)
                .toList();

        return toResponse(category, subCategories);
    }

    private CategoryResponse toChildResponse(Category category) {
        return toResponse(category, List.of());
    }

    private CategoryResponse toResponse(Category category, List<CategoryResponse> subCategories) {
        return new CategoryResponse(
                category.getId(),
                category.getParentCategory() != null ? category.getParentCategory().getId() : null,
                category.getLevel(),
                category.getName(),
                category.getDescription(),
                category.getIcon(),
                category.getTextColor(),
                category.getBackgroundColor(),
                category.getMonthlyBudget(),
                category.isActive(),
                category.isAllowOverBudget(),
                category.getTenantId(),
                subCategories
        );
    }

    private String normalizeName(String name) {
        String trimmed = name == null ? null : name.trim();
        if (trimmed == null || trimmed.isBlank()) {
            throw new BadRequestException("Category name is required");
        }
        return trimmed;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void logActivity(ActivityAction action, CategoryResponse category, String message, String metadata) {
        activityLogService.record(
                action,
                ActivitySubjectType.CATEGORY,
                category.id(),
                category.name(),
                message,
                metadata
        );
    }

    private String buildActivityMessage(String verb) {
        return "Category " + verb;
    }

    private String buildActivityMetadata(CategoryResponse category) {
        return "{" +
                "\"id\":" + category.id() + ',' +
                "\"parentCategoryId\":" + (category.parentCategoryId() == null ? "null" : category.parentCategoryId()) + ',' +
                "\"level\":" + (category.level() == null ? "null" : category.level()) + ',' +
                "\"name\":\"" + escapeJson(category.name()) + "\"," +
                "\"active\":" + category.active() + ',' +
                "\"allowOverBudget\":" + category.allowOverBudget() +
                "}";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

