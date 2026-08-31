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
        Long tenantId = TenantContext.getRequiredTenantId();
        return categoryRepository.findAllByTenantIdOrderByNameAsc(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        return toResponse(loadCategory(id, tenantId));
    }

    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        String normalizedName = normalizeName(request.name());
        ensureNameIsAvailable(tenantId, normalizedName, null);

        Category category = new Category();
        applyRequest(category, request);
        category.setName(normalizedName);

        CategoryResponse response = toResponse(categoryRepository.save(category));
        logActivity(ActivityAction.CREATE, response, buildActivityMessage("created"), buildActivityMetadata(response));
        return response;
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryCreateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Category category = loadCategory(id, tenantId);
        String normalizedName = normalizeName(request.name());
        ensureNameIsAvailable(tenantId, normalizedName, id);

        applyRequest(category, request);
        category.setName(normalizedName);

        CategoryResponse response = toResponse(categoryRepository.save(category));
        logActivity(ActivityAction.UPDATE, response, buildActivityMessage("updated"), buildActivityMetadata(response));
        return response;
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Category category = loadCategory(id, tenantId);
        CategoryResponse response = toResponse(category);
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

    private void applyRequest(Category category, CategoryCreateRequest request) {
        category.setDescription(normalizeOptional(request.description()));
        category.setIcon(normalizeOptional(request.icon()));
        category.setTextColor(normalizeOptional(request.textColor()));
        category.setBackgroundColor(normalizeOptional(request.backgroundColor()));
        category.setMonthlyBudget(request.monthlyBudget());
        category.setActive(Boolean.TRUE.equals(request.active()));
        category.setAllowOverBudget(Boolean.TRUE.equals(request.allowOverBudget()));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getIcon(),
                category.getTextColor(),
                category.getBackgroundColor(),
                category.getMonthlyBudget(),
                category.isActive(),
                category.isAllowOverBudget(),
                category.getTenantId()
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

