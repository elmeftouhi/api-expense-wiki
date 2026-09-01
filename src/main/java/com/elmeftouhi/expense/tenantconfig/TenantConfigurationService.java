package com.elmeftouhi.expense.tenantconfig;

import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.shared.exception.ConflictException;
import com.elmeftouhi.expense.shared.exception.ResourceNotFoundException;
import com.elmeftouhi.expense.tenantconfig.dto.TenantConfigurationRequest;
import com.elmeftouhi.expense.tenantconfig.dto.TenantConfigurationResponse;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantConfigurationService {

    private final TenantConfigurationRepository configRepository;

    // Cache with simple TTL (5 minutes)
    private static final long CACHE_TTL_MINUTES = 5;
    private static final Map<Long, CacheEntry<TenantConfiguration>> CONFIG_CACHE = new ConcurrentHashMap<>();

    private static class CacheEntry<T> {
        T value;
        long timestamp;

        CacheEntry(T value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > TimeUnit.MINUTES.toMillis(CACHE_TTL_MINUTES);
        }
    }

    @Transactional(readOnly = true)
    public TenantConfigurationResponse getConfiguration() {
        Long tenantId = TenantContext.getRequiredTenantId();
        TenantConfiguration config = loadConfiguration(tenantId);
        return toResponse(config);
    }

    @Transactional
    public TenantConfigurationResponse updateConfiguration(TenantConfigurationRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        TenantConfiguration config = loadConfiguration(tenantId);

        applyRequest(config, request);
        config.setUpdatedAt(Instant.now());
        TenantConfiguration saved = configRepository.save(config);

        // Invalidate cache
        CONFIG_CACHE.remove(tenantId);

        return toResponse(saved);
    }

    @Transactional
    public TenantConfigurationResponse createDefaultConfiguration(Long tenantId) {
        if (configRepository.findByTenantId(tenantId).isPresent()) {
            throw new ConflictException("Configuration already exists for this tenant");
        }

        TenantConfiguration config = new TenantConfiguration();
        config.assignTenantId(tenantId);
        TenantConfiguration saved = configRepository.save(config);

        // Invalidate cache
        CONFIG_CACHE.remove(tenantId);

        return toResponse(saved);
    }

    public TenantConfiguration loadConfiguration(Long tenantId) {
        // Check cache
        CacheEntry<TenantConfiguration> cached = CONFIG_CACHE.get(tenantId);
        if (cached != null && !cached.isExpired()) {
            return cached.value;
        }

        // Load from database
        TenantConfiguration config = configRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant configuration not found"));

        // Update cache
        CONFIG_CACHE.put(tenantId, new CacheEntry<>(config));
        return config;
    }

    public void invalidateCache(Long tenantId) {
        CONFIG_CACHE.remove(tenantId);
    }

    @Scheduled(fixedRateString = "${app.config.cache-cleanup-interval:300000}") // 5 minutes default
    public void cleanupExpiredCache() {
        CONFIG_CACHE.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private void applyRequest(TenantConfiguration config, TenantConfigurationRequest request) {
        if (request.currency() != null) {
            config.setCurrency(request.currency());
        }
        if (request.decimalPlaces() != null) {
            validateDecimalPlaces(request.decimalPlaces());
            config.setDecimalPlaces(request.decimalPlaces());
        }
        if (request.language() != null) {
            config.setLanguage(request.language());
        }
        if (request.timezone() != null) {
            validateTimezone(request.timezone());
            config.setTimezone(request.timezone());
        }
        if (request.dateFormat() != null) {
            config.setDateFormat(request.dateFormat());
        }
        if (request.timeFormat() != null) {
            config.setTimeFormat(request.timeFormat());
        }
        if (request.weekStartDay() != null) {
            config.setWeekStartDay(request.weekStartDay());
        }
        if (request.budgetAlertThreshold() != null) {
            validateBudgetThreshold(request.budgetAlertThreshold());
            config.setBudgetAlertThreshold(request.budgetAlertThreshold());
        }
        if (request.notificationChannels() != null) {
            validateJsonArray(request.notificationChannels());
            config.setNotificationChannels(request.notificationChannels());
        }
        if (request.autoPaymentReminderDays() != null) {
            validateJsonArray(request.autoPaymentReminderDays());
            config.setAutoPaymentReminderDays(request.autoPaymentReminderDays());
        }
        if (request.expenseRoundingMethod() != null) {
            config.setExpenseRoundingMethod(request.expenseRoundingMethod());
        }
        if (request.reportingPeriod() != null) {
            config.setReportingPeriod(request.reportingPeriod());
        }
    }

    private void validateDecimalPlaces(Integer decimalPlaces) {
        if (decimalPlaces < 0 || decimalPlaces > 8) {
            throw new IllegalArgumentException("Decimal places must be between 0 and 8");
        }
    }

    private void validateBudgetThreshold(Integer threshold) {
        if (threshold < 0 || threshold > 100) {
            throw new IllegalArgumentException("Budget alert threshold must be between 0 and 100");
        }
    }

    private void validateTimezone(String timezone) {
        try {
            java.time.ZoneId.of(timezone);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid timezone: " + timezone);
        }
    }

    private void validateJsonArray(String json) {
        if (json == null || !json.trim().startsWith("[")) {
            throw new IllegalArgumentException("Must be a valid JSON array");
        }
    }

    private TenantConfigurationResponse toResponse(TenantConfiguration config) {
        return new TenantConfigurationResponse(
                config.getId(),
                config.getTenantId(),
                config.getCurrency(),
                config.getDecimalPlaces(),
                config.getLanguage(),
                config.getTimezone(),
                config.getDateFormat(),
                config.getTimeFormat(),
                config.getWeekStartDay(),
                config.getBudgetAlertThreshold(),
                config.getNotificationChannels(),
                config.getAutoPaymentReminderDays(),
                config.getExpenseRoundingMethod(),
                config.getReportingPeriod(),
                config.getCreatedAt(),
                config.getUpdatedAt(),
                config.getCreatedBy(),
                config.getUpdatedBy()
        );
    }
}

