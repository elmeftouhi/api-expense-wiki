package com.elmeftouhi.expense.paymentmethod.service;

import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.security.TenantPermissionService;
import com.elmeftouhi.expense.paymentmethod.dto.PaymentMethodRequest;
import com.elmeftouhi.expense.paymentmethod.dto.PaymentMethodResponse;
import com.elmeftouhi.expense.paymentmethod.entity.PaymentMethod;
import com.elmeftouhi.expense.paymentmethod.repository.PaymentMethodRepository;
import com.elmeftouhi.expense.shared.exception.BadRequestException;
import com.elmeftouhi.expense.shared.exception.ConflictException;
import com.elmeftouhi.expense.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final TenantPermissionService tenantPermissionService;

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> findAll(boolean includeInactive) {
        Long tenantId = TenantContext.getRequiredTenantId();
        return paymentMethodRepository.findByTenantIdOrderByIsDefaultDescNameAsc(tenantId).stream()
                .filter(method -> includeInactive || method.isActive())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PaymentMethodResponse create(PaymentMethodRequest request) {
        tenantPermissionService.requireEditor();
        Long tenantId = TenantContext.getRequiredTenantId();
        String normalizedName = normalizeName(request.name());

        paymentMethodRepository.findByTenantIdAndNameIgnoreCase(tenantId, normalizedName)
                .ifPresent(existing -> {
                    throw new ConflictException("Payment method name already exists");
                });

        boolean active = request.active() == null || request.active();
        boolean hasDefault = paymentMethodRepository.findByTenantIdAndIsDefaultTrue(tenantId).isPresent();
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault()) || !hasDefault;

        if (makeDefault && !active) {
            throw new BadRequestException("Default payment method must be active");
        }

        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.assignTenantId(tenantId);
        paymentMethod.setName(normalizedName);
        paymentMethod.setType(request.type());
        paymentMethod.setActive(active);
        paymentMethod.setDefault(makeDefault);

        if (makeDefault) {
            paymentMethodRepository.findByTenantIdAndIsDefaultTrue(tenantId)
                    .ifPresent(existingDefault -> {
                        existingDefault.setDefault(false);
                        existingDefault.setUpdatedAt(Instant.now());
                        paymentMethodRepository.save(existingDefault);
                    });
        }

        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);
        return toResponse(saved);
    }

    @Transactional
    public PaymentMethodResponse update(Long id, PaymentMethodRequest request) {
        tenantPermissionService.requireEditor();
        Long tenantId = TenantContext.getRequiredTenantId();
        PaymentMethod paymentMethod = loadPaymentMethod(id, tenantId);
        String normalizedName = normalizeName(request.name());

        paymentMethodRepository.findByTenantIdAndNameIgnoreCase(tenantId, normalizedName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Payment method name already exists");
                });

        boolean active = request.active() == null ? paymentMethod.isActive() : request.active();
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault());

        if (makeDefault && !active) {
            throw new BadRequestException("Default payment method must be active");
        }
        if (paymentMethod.isDefault() && !active) {
            throw new BadRequestException("Default payment method cannot be deactivated");
        }
        if (Boolean.FALSE.equals(request.isDefault()) && paymentMethod.isDefault()) {
            throw new BadRequestException("Use set-default endpoint to switch the default payment method");
        }

        paymentMethod.setName(normalizedName);
        paymentMethod.setType(request.type());
        paymentMethod.setActive(active);
        paymentMethod.setUpdatedAt(Instant.now());

        if (makeDefault && !paymentMethod.isDefault()) {
            paymentMethodRepository.findByTenantIdAndIsDefaultTrue(tenantId)
                    .ifPresent(existingDefault -> {
                        existingDefault.setDefault(false);
                        existingDefault.setUpdatedAt(Instant.now());
                        paymentMethodRepository.save(existingDefault);
                    });
            paymentMethod.setDefault(true);
        }

        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);
        return toResponse(saved);
    }

    @Transactional
    public PaymentMethodResponse setDefault(Long id) {
        tenantPermissionService.requireEditor();
        Long tenantId = TenantContext.getRequiredTenantId();
        PaymentMethod paymentMethod = loadPaymentMethod(id, tenantId);

        if (!paymentMethod.isActive()) {
            throw new BadRequestException("Only active payment methods can be selected as default");
        }

        if (paymentMethod.isDefault()) {
            return toResponse(paymentMethod);
        }

        paymentMethodRepository.findByTenantIdAndIsDefaultTrue(tenantId)
                .ifPresent(existingDefault -> {
                    existingDefault.setDefault(false);
                    existingDefault.setUpdatedAt(Instant.now());
                    paymentMethodRepository.save(existingDefault);
                });

        paymentMethod.setDefault(true);
        paymentMethod.setUpdatedAt(Instant.now());
        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaymentMethod resolveForUsage(Long tenantId, Long paymentMethodId) {
        if (paymentMethodId != null) {
            PaymentMethod selected = loadPaymentMethod(paymentMethodId, tenantId);
            if (!selected.isActive()) {
                throw new BadRequestException("Selected payment method is inactive");
            }
            return selected;
        }

        PaymentMethod defaultMethod = paymentMethodRepository.findByTenantIdAndIsDefaultTrue(tenantId)
                .orElseThrow(() -> new BadRequestException("No default payment method configured"));

        if (!defaultMethod.isActive()) {
            throw new BadRequestException("Default payment method is inactive");
        }

        return defaultMethod;
    }

    @Transactional(readOnly = true)
    public PaymentMethod resolveOptionalForUsage(Long tenantId, Long paymentMethodId) {
        if (paymentMethodId == null) {
            return null;
        }

        PaymentMethod selected = loadPaymentMethod(paymentMethodId, tenantId);
        if (!selected.isActive()) {
            throw new BadRequestException("Selected payment method is inactive");
        }
        return selected;
    }

    private PaymentMethod loadPaymentMethod(Long id, Long tenantId) {
        return paymentMethodRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found"));
    }

    private String normalizeName(String name) {
        String trimmed = name == null ? null : name.trim();
        if (trimmed == null || trimmed.isBlank()) {
            throw new BadRequestException("Payment method name is required");
        }
        return trimmed;
    }

    private PaymentMethodResponse toResponse(PaymentMethod paymentMethod) {
        return new PaymentMethodResponse(
                paymentMethod.getId(),
                paymentMethod.getName(),
                paymentMethod.getType(),
                paymentMethod.isActive(),
                paymentMethod.isDefault(),
                paymentMethod.getTenantId(),
                paymentMethod.getCreatedAt(),
                paymentMethod.getUpdatedAt()
        );
    }
}

