package com.elmeftouhi.expense.auth;

import com.elmeftouhi.expense.auth.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;

import com.elmeftouhi.expense.auth.dto.AuthResponse;
import com.elmeftouhi.expense.auth.dto.LoginRequest;
import com.elmeftouhi.expense.auth.dto.RegisterRequest;
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
}

