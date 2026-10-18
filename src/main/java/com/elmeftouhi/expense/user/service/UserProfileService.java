package com.elmeftouhi.expense.user.service;

import com.elmeftouhi.expense.user.entity.AppUser;
import com.elmeftouhi.expense.user.repository.AppUserRepository;
import com.elmeftouhi.expense.user.entity.UserTenant;
import com.elmeftouhi.expense.user.repository.UserTenantRepository;
import com.elmeftouhi.expense.user.TenantRole;
import com.elmeftouhi.expense.security.JwtPrincipal;
import com.elmeftouhi.expense.shared.exception.BadRequestException;
import com.elmeftouhi.expense.shared.exception.ResourceNotFoundException;
import com.elmeftouhi.expense.tenant.entity.Tenant;
import com.elmeftouhi.expense.tenant.dto.TenantMembershipResponse;
import com.elmeftouhi.expense.user.dto.MeResponse;
import com.elmeftouhi.expense.user.dto.PasswordChangeRequest;
import com.elmeftouhi.expense.user.dto.UserProfileUpdateRequest;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final AppUserRepository appUserRepository;
    private final UserTenantRepository userTenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public MeResponse me(JwtPrincipal principal) {
        AppUser user = appUserRepository.findById(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String displayedName = buildDisplayedName(user);

        return new MeResponse(
                user.getId(),
                user.getEmail(),
                displayedName,
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getStatus(),
                user.getDefaultTenant().getId(),
                principal.selectedTenantId(),
                user.getRoles()
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
        } else {
            return user.getEmail();
        }
    }

    @Transactional(readOnly = true)
    public List<TenantMembershipResponse> myTenants(JwtPrincipal principal) {
        AppUser user = appUserRepository.findById(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long defaultTenantId = user.getDefaultTenant().getId();
        return userTenantRepository.findAllByUserId(principal.userId())
                .stream()
                .map(userTenant -> new TenantMembershipResponse(
                        userTenant.getTenant().getId(),
                        userTenant.getTenant().getName(),
                        userTenant.getTenant().getId().equals(defaultTenantId),
                        resolveTenantRole(userTenant)
                ))
                .toList();
    }

    @Transactional
    public MeResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.firstName() != null) {
            user.setFirstName(request.firstName().trim().isEmpty() ? null : request.firstName().trim());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName().trim().isEmpty() ? null : request.lastName().trim());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone().trim().isEmpty() ? null : request.phone().trim());
        }

        if (request.defaultTenantId() != null) {
            Tenant tenant = userTenantRepository.findAllByUserId(userId).stream()
                    .filter(ut -> ut.getTenant().getId().equals(request.defaultTenantId()))
                    .map(ut -> ut.getTenant())
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("User does not have membership access to this tenant"));
            user.setDefaultTenant(tenant);
        }

        user.setUpdatedAt(java.time.Instant.now());
        AppUser savedUser = appUserRepository.save(user);

        return new MeResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                buildDisplayedName(savedUser),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getPhone(),
                savedUser.getStatus(),
                savedUser.getDefaultTenant().getId(),
                savedUser.getDefaultTenant().getId(),
                savedUser.getRoles()
        );
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

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password does not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(java.time.Instant.now());
        appUserRepository.save(user);
    }
}
