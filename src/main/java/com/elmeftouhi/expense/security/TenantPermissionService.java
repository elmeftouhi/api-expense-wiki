package com.elmeftouhi.expense.security;

import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.shared.exception.TenantAccessDeniedException;
import com.elmeftouhi.expense.user.TenantRole;
import com.elmeftouhi.expense.user.repository.UserTenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Database-backed tenant permissions, independent of global system roles. */
@Service
@RequiredArgsConstructor
public class TenantPermissionService {

    private final UserTenantRepository userTenantRepository;

    public void requireEditor() {
        requireCurrentRole(TenantRole.EDITOR);
    }

    public void requireAdmin() {
        requireCurrentRole(TenantRole.ADMIN);
    }

    /** For operations with an explicit target tenant and a server-resolved caller ID. */
    public boolean hasRole(Long userId, Long tenantId, TenantRole requiredRole) {
        if (userId == null || tenantId == null || requiredRole == null) {
            return false;
        }
        return userTenantRepository.findByUserIdAndTenantId(userId, tenantId)
                .map(membership -> membership.getTenantRole())
                .map(role -> role.atLeast(requiredRole))
                .orElse(false);
    }

    private void requireCurrentRole(TenantRole requiredRole) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long tenantId = TenantContext.getTenantId();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof JwtPrincipal principal)
                || tenantId == null || !tenantId.equals(principal.selectedTenantId())
                || !hasRole(principal.userId(), tenantId, requiredRole)) {
            throw new TenantAccessDeniedException(
                    "This operation requires " + requiredRole + " access to the selected tenant");
        }
    }
}
