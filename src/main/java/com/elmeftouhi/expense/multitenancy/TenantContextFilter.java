package com.elmeftouhi.expense.multitenancy;

import com.elmeftouhi.expense.security.JwtPrincipal;
import com.elmeftouhi.expense.tenantconfig.ConfigurationContext;
import com.elmeftouhi.expense.tenantconfig.service.TenantConfigurationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class TenantContextFilter extends OncePerRequestFilter {

    private final TenantConfigurationService tenantConfigurationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof JwtPrincipal principal) {
                TenantContext.setTenantId(principal.selectedTenantId());
                ConfigurationContext.setConfiguration(
                        tenantConfigurationService.loadConfiguration(principal.selectedTenantId())
                );
            }
            filterChain.doFilter(request, response);
        } finally {
            ConfigurationContext.clear();
            TenantContext.clear();
        }
    }
}

