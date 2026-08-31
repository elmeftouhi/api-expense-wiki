package com.elmeftouhi.expense.user;

import com.elmeftouhi.expense.shared.exception.BadRequestException;
import com.elmeftouhi.expense.shared.exception.ConflictException;
import com.elmeftouhi.expense.shared.exception.ResourceNotFoundException;
import com.elmeftouhi.expense.shared.exception.TenantAccessDeniedException;
import com.elmeftouhi.expense.tenant.Tenant;
import com.elmeftouhi.expense.tenant.TenantRepository;
import com.elmeftouhi.expense.tenant.dto.TenantMembershipResponse;
import com.elmeftouhi.expense.user.dto.TenantAssignmentRequest;
import com.elmeftouhi.expense.user.dto.UserCreateRequest;
import com.elmeftouhi.expense.user.dto.UserManagementResponse;
import com.elmeftouhi.expense.user.dto.UserUpdateRequest;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final AppUserRepository appUserRepository;
    private final TenantRepository tenantRepository;
    private final UserTenantRepository userTenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserManagementResponse> listUsers(Long callerUserId) {
        ensureSystemAdmin(callerUserId);
        return appUserRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserManagementResponse getUser(Long callerUserId, Long userId) {
        ensureSystemAdmin(callerUserId);
        return toResponse(loadUser(userId));
    }

    @Transactional
    public UserManagementResponse createUser(Long callerUserId, UserCreateRequest request) {
        ensureSystemAdmin(callerUserId);

        String normalizedEmail = normalizeEmail(request.email());
        if (appUserRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ConflictException("Email already exists");
        }

        Map<Long, TenantRole> assignments = normalizeAssignments(request.tenantAssignments());
        Long defaultTenantId = resolveDefaultTenantId(request.defaultTenantId(), assignments);

        AppUser user = new AppUser();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(normalizeText(request.firstName()));
        user.setLastName(normalizeText(request.lastName()));
        user.setPhone(normalizeText(request.phone()));
        user.setStatus(request.status() != null ? request.status() : UserStatus.ACTIVE);
        user.getRoles().add(Role.USER);
        user = appUserRepository.save(user);

        attachMemberships(user, assignments);
        user.setDefaultTenant(resolveTenant(defaultTenantId));
        user.setUpdatedAt(Instant.now());
        user = appUserRepository.save(user);

        return toResponse(user);
    }

    @Transactional
    public UserManagementResponse updateUser(Long callerUserId, Long userId, UserUpdateRequest request) {
        ensureSystemAdmin(callerUserId);

        AppUser user = loadUser(userId);

        if (request.email() != null) {
            String normalizedEmail = normalizeEmail(request.email());
            appUserRepository.findByEmail(normalizedEmail)
                    .filter(existing -> !existing.getId().equals(userId))
                    .ifPresent(existing -> {
                        throw new ConflictException("Email already exists");
                    });
            user.setEmail(normalizedEmail);
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.firstName() != null) {
            user.setFirstName(normalizeText(request.firstName()));
        }
        if (request.lastName() != null) {
            user.setLastName(normalizeText(request.lastName()));
        }
        if (request.phone() != null) {
            user.setPhone(normalizeText(request.phone()));
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }

        Map<Long, TenantRole> assignments = request.tenantAssignments() != null
                ? normalizeAssignments(request.tenantAssignments())
                : null;

        if (assignments != null) {
            if (assignments.isEmpty()) {
                throw new BadRequestException("At least one tenant assignment is required");
            }
            if (request.defaultTenantId() != null && !assignments.containsKey(request.defaultTenantId())) {
                throw new BadRequestException("Default tenant must be included in tenant assignments");
            }

            userTenantRepository.deleteAllByUserId(userId);
            attachMemberships(user, assignments);

            Long defaultTenantId = request.defaultTenantId() != null ? request.defaultTenantId() : assignments.keySet().iterator().next();
            user.setDefaultTenant(resolveTenant(defaultTenantId));
        } else if (request.defaultTenantId() != null) {
            if (!userTenantRepository.existsByUserIdAndTenantId(userId, request.defaultTenantId())) {
                throw new BadRequestException("User does not have membership access to this tenant");
            }
            user.setDefaultTenant(resolveTenant(request.defaultTenantId()));
        }

        user.setUpdatedAt(Instant.now());
        user = appUserRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public void deleteUser(Long callerUserId, Long userId) {
        ensureSystemAdmin(callerUserId);
        if (callerUserId.equals(userId)) {
            throw new BadRequestException("Self-deletion is not allowed");
        }

        AppUser user = loadUser(userId);
        boolean hasAdminMembership = userTenantRepository.findAllByUserId(userId).stream()
                .anyMatch(userTenant -> resolveTenantRole(userTenant).atLeast(TenantRole.ADMIN));
        if (hasAdminMembership) {
            throw new ConflictException("Cannot delete a user with ADMIN access on a tenant. Reassign roles first");
        }

        userTenantRepository.deleteAllByUserId(userId);
        appUserRepository.delete(user);
    }

    private void ensureSystemAdmin(Long callerUserId) {
        AppUser caller = appUserRepository.findWithDefaultTenantById(callerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!caller.getRoles().contains(Role.ADMIN)) {
            throw new TenantAccessDeniedException("Only system admins can manage users");
        }
    }

    private AppUser loadUser(Long userId) {
        return appUserRepository.findWithDefaultTenantById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserManagementResponse toResponse(AppUser user) {
        List<TenantMembershipResponse> tenantMemberships = userTenantRepository.findAllByUserId(user.getId()).stream()
                .map(this::toTenantMembershipResponse)
                .sorted(Comparator.comparing(TenantMembershipResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new UserManagementResponse(
                user.getId(),
                user.getEmail(),
                buildDisplayedName(user),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getStatus(),
                user.getDefaultTenant().getId(),
                user.getDefaultTenant().getName(),
                user.getRoles(),
                tenantMemberships
        );
    }

    private TenantMembershipResponse toTenantMembershipResponse(UserTenant userTenant) {
        Tenant tenant = userTenant.getTenant();
        return new TenantMembershipResponse(
                tenant.getId(),
                tenant.getName(),
                userTenant.getUser().getDefaultTenant() != null && tenant.getId().equals(userTenant.getUser().getDefaultTenant().getId()),
                resolveTenantRole(userTenant)
        );
    }

    private String buildDisplayedName(AppUser user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();

        if (firstName != null && !firstName.isBlank() && lastName != null && !lastName.isBlank()) {
            return firstName + " " + lastName;
        } else if (firstName != null && !firstName.isBlank()) {
            return firstName;
        } else if (lastName != null && !lastName.isBlank()) {
            return lastName;
        }
        return user.getEmail();
    }

    private Map<Long, TenantRole> normalizeAssignments(List<TenantAssignmentRequest> assignments) {
        Map<Long, TenantRole> normalized = new LinkedHashMap<>();
        for (TenantAssignmentRequest assignment : assignments) {
            if (assignment == null || assignment.tenantId() == null) {
                throw new BadRequestException("Each tenant assignment must include a tenant id");
            }
            if (assignment.role() == null) {
                throw new BadRequestException("Each tenant assignment must include a role");
            }
            if (normalized.putIfAbsent(assignment.tenantId(), assignment.role()) != null) {
                throw new BadRequestException("Duplicate tenant assignment detected");
            }
            resolveTenant(assignment.tenantId());
        }
        return normalized;
    }

    private Long resolveDefaultTenantId(Long requestedDefaultTenantId, Map<Long, TenantRole> assignments) {
        if (requestedDefaultTenantId != null) {
            if (!assignments.containsKey(requestedDefaultTenantId)) {
                throw new BadRequestException("Default tenant must be included in tenant assignments");
            }
            return requestedDefaultTenantId;
        }
        return assignments.keySet().iterator().next();
    }

    private void attachMemberships(AppUser user, Map<Long, TenantRole> assignments) {
        for (Map.Entry<Long, TenantRole> entry : assignments.entrySet()) {
            UserTenant membership = new UserTenant();
            membership.setUser(user);
            membership.setTenant(resolveTenant(entry.getKey()));
            membership.setTenantRole(entry.getValue());
            userTenantRepository.save(membership);
        }
    }

    private Tenant resolveTenant(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    }

    private TenantRole resolveTenantRole(UserTenant userTenant) {
        if (userTenant.getTenantRole() != null) {
            return userTenant.getTenantRole();
        }
        if (userTenant.getUser() != null
                && userTenant.getUser().getDefaultTenant() != null
                && userTenant.getTenant() != null
                && userTenant.getUser().getDefaultTenant().getId().equals(userTenant.getTenant().getId())) {
            return TenantRole.ADMIN;
        }
        return TenantRole.VIEWER;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

