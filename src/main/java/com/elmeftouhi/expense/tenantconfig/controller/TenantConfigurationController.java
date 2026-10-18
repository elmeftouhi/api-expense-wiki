package com.elmeftouhi.expense.tenantconfig.controller;

import com.elmeftouhi.expense.tenantconfig.service.TenantConfigurationService;
import com.elmeftouhi.expense.tenantconfig.dto.TenantConfigurationRequest;
import com.elmeftouhi.expense.tenantconfig.dto.TenantConfigurationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/config")
@RequiredArgsConstructor
public class TenantConfigurationController {

    private final TenantConfigurationService configService;

    @GetMapping
    public TenantConfigurationResponse getConfiguration() {
        return configService.getConfiguration();
    }

    @PutMapping
    public TenantConfigurationResponse updateConfiguration(
            @Valid @RequestBody TenantConfigurationRequest request) {
        return configService.updateConfiguration(request);
    }
}

