package com.elmeftouhi.expense.auth;

import com.elmeftouhi.expense.auth.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.elmeftouhi.expense.auth.dto.AuthResponse;
import com.elmeftouhi.expense.auth.dto.LoginRequest;
import com.elmeftouhi.expense.auth.dto.RegisterRequest;
import com.elmeftouhi.expense.shared.exception.ConflictException;
import com.elmeftouhi.expense.shared.exception.TenantAccessDeniedException;
import com.elmeftouhi.expense.tenant.repository.TenantRepository;
import com.elmeftouhi.expense.user.Role;
import com.elmeftouhi.expense.user.TenantRole;
import com.elmeftouhi.expense.user.entity.AppUser;
import com.elmeftouhi.expense.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTenantSwitchTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void switchTenantUpdatesDefaultTenantAndIsUsedOnNextLogin() {
        long unique = System.nanoTime();
        String email = "switch-tenant-" + unique + "@example.com";
        String password = "Password123!";

        AuthResponse registerResponse = authService.register(new RegisterRequest(
                email,
                password,
                "Tenant-" + unique,
                "Test",
                "User",
                null
        ));

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Registered user not found"));

        AuthResponse createTenantResponse = authService.createTenantForUser(user.getId(), "Tenant-Extra-" + unique);
        Long switchedTenantId = createTenantResponse.selectedTenantId();

        authService.switchTenant(user.getId(), switchedTenantId);

        AppUser reloadedUser = appUserRepository.findWithDefaultTenantById(user.getId())
                .orElseThrow(() -> new IllegalStateException("User not found after switch"));
        assertThat(reloadedUser.getDefaultTenant().getId()).isEqualTo(switchedTenantId);

        AuthResponse loginResponse = authService.login(new LoginRequest(email, password));
        assertThat(loginResponse.selectedTenantId()).isEqualTo(switchedTenantId);
        assertThat(loginResponse.allowedTenantIds())
                .contains(registerResponse.selectedTenantId(), switchedTenantId);
    }

    @Test
    void tenantAdminCanUpdateTenantName() {
        long unique = System.nanoTime();
        String email = "tenant-update-admin-" + unique + "@example.com";

        AuthResponse registerResponse = authService.register(new RegisterRequest(
                email,
                "Password123!",
                "Tenant-Admin-" + unique,
                "Admin",
                "User",
                null
        ));

        AppUser admin = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Registered user not found"));

        String updatedName = "Tenant-Admin-Updated-" + unique;
        authService.updateTenantForUser(admin.getId(), registerResponse.selectedTenantId(), updatedName);

        String reloadedName = tenantRepository.findById(registerResponse.selectedTenantId())
                .orElseThrow(() -> new IllegalStateException("Tenant not found after update"))
                .getName();
        assertThat(reloadedName).isEqualTo(updatedName);
    }

    @Test
    void tenantViewerCannotUpdateTenantName() {
        long unique = System.nanoTime();

        String ownerEmail = "tenant-owner-" + unique + "@example.com";
        AuthResponse ownerRegister = authService.register(new RegisterRequest(
                ownerEmail,
                "Password123!",
                "Tenant-Owner-" + unique,
                "Owner",
                "User",
                null
        ));
        AppUser owner = appUserRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new IllegalStateException("Owner not found"));

        String viewerEmail = "tenant-viewer-" + unique + "@example.com";
        authService.register(new RegisterRequest(
                viewerEmail,
                "Password123!",
                "Tenant-Viewer-Home-" + unique,
                "Viewer",
                "User",
                null
        ));
        AppUser viewer = appUserRepository.findByEmail(viewerEmail)
                .orElseThrow(() -> new IllegalStateException("Viewer not found"));
        viewer.getRoles().remove(Role.ADMIN);
        appUserRepository.save(viewer);

        String inviteCode = authService.createTenantInviteForUser(
                owner.getId(),
                ownerRegister.selectedTenantId(),
                2,
                TenantRole.VIEWER
        ).inviteCode();
        authService.joinTenantForUser(viewer.getId(), inviteCode);

        assertThatThrownBy(() -> authService.updateTenantForUser(
                viewer.getId(),
                ownerRegister.selectedTenantId(),
                "Blocked-Update-" + unique
        ))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("Only tenant admins can update tenant info");
    }

    @Test
    void updateTenantRejectsDuplicateTenantName() {
        long unique = System.nanoTime();
        String email = "tenant-update-dup-" + unique + "@example.com";

        AuthResponse registerResponse = authService.register(new RegisterRequest(
                email,
                "Password123!",
                "Tenant-Primary-" + unique,
                "Test",
                "User",
                null
        ));

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Registered user not found"));

        String existingTenantName = "Tenant-Secondary-" + unique;
        authService.createTenantForUser(user.getId(), existingTenantName);

        assertThatThrownBy(() -> authService.updateTenantForUser(
                user.getId(),
                registerResponse.selectedTenantId(),
                existingTenantName
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Tenant name already exists");
    }
}

