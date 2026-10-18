package com.elmeftouhi.expense.tenantconfig;

import com.elmeftouhi.expense.tenantconfig.entity.TenantConfiguration;
import com.elmeftouhi.expense.tenantconfig.repository.TenantConfigurationRepository;
import com.elmeftouhi.expense.tenantconfig.service.TenantConfigurationService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.shared.exception.ConflictException;
import com.elmeftouhi.expense.tenantconfig.dto.TenantConfigurationRequest;
import com.elmeftouhi.expense.tenantconfig.dto.TenantConfigurationResponse;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantConfigurationService Tests")
class TenantConfigurationServiceTests {

    @Mock
    private TenantConfigurationRepository configRepository;

    @InjectMocks
    private TenantConfigurationService configService;

    private Long tenantId = 1L;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(tenantId);
        configService.invalidateCache(tenantId);
    }

    @Test
    @DisplayName("Should get default configuration")
    void testGetConfiguration() {
        // Given
        TenantConfiguration config = new TenantConfiguration();
        config.setId(1L);
        config.assignTenantId(tenantId);
        config.setCurrency(Currency.USD);
        config.setLanguage(Language.EN);
        config.setDecimalPlaces(2);

        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config));

        // When
        TenantConfigurationResponse response = configService.getConfiguration();

        // Then
        assertNotNull(response);
        assertEquals(tenantId, response.tenantId());
        assertEquals(Currency.USD, response.currency());
        assertEquals(Language.EN, response.language());
        assertEquals(2, response.decimalPlaces());
    }

    @Test
    @DisplayName("Should auto-create configuration when not found")
    void testGetConfigurationNotFound() {
        // Given
        TenantConfiguration config = new TenantConfiguration();
        config.setId(10L);
        config.assignTenantId(tenantId);

        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(configRepository.save(any(TenantConfiguration.class))).thenReturn(config);

        // When
        TenantConfigurationResponse response = configService.getConfiguration();

        // Then
        assertNotNull(response);
        assertEquals(tenantId, response.tenantId());
        verify(configRepository).save(any(TenantConfiguration.class));
    }

    @Test
    @DisplayName("Should auto-create then update configuration when not found")
    void testUpdateConfigurationAutoCreatesWhenMissing() {
        // Given
        TenantConfiguration config = new TenantConfiguration();
        config.setId(20L);
        config.assignTenantId(tenantId);

        TenantConfigurationRequest request = new TenantConfigurationRequest(
                Currency.EUR,
                2,
                Language.FR,
                "Europe/Paris",
                "dd/MM/yyyy",
                TimeFormat.FORMAT_24H,
                WeekStartDay.MONDAY,
                80,
                "[\"IN_APP\"]",
                "[3,7]",
                "NONE",
                "CALENDAR_MONTH"
        );

        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(configRepository.save(any(TenantConfiguration.class))).thenReturn(config);

        // When
        TenantConfigurationResponse response = configService.updateConfiguration(request);

        // Then
        assertNotNull(response);
        verify(configRepository, times(2)).save(any(TenantConfiguration.class));
    }

    @Test
    @DisplayName("Should update configuration")
    void testUpdateConfiguration() {
        // Given
        TenantConfiguration config = new TenantConfiguration();
        config.setId(1L);
        config.assignTenantId(tenantId);
        config.setCurrency(Currency.USD);
        config.setLanguage(Language.EN);
        config.setDecimalPlaces(2);

        TenantConfigurationRequest request = new TenantConfigurationRequest(
                Currency.EUR,
                3,
                Language.FR,
                "Europe/Paris",
                "dd/MM/yyyy",
                TimeFormat.FORMAT_24H,
                WeekStartDay.MONDAY,
                80,
                "[\"IN_APP\"]",
                "[3,7]",
                "NONE",
                "CALENDAR_MONTH"
        );

        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config));
        when(configRepository.save(any(TenantConfiguration.class))).thenReturn(config);

        // When
        TenantConfigurationResponse response = configService.updateConfiguration(request);

        // Then
        assertNotNull(response);
        verify(configRepository, times(1)).save(any(TenantConfiguration.class));
    }

    @Test
    @DisplayName("Should create default configuration")
    void testCreateDefaultConfiguration() {
        // Given
        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        TenantConfiguration config = new TenantConfiguration();
        config.setId(1L);
        config.assignTenantId(tenantId);

        when(configRepository.save(any(TenantConfiguration.class))).thenReturn(config);

        // When
        TenantConfigurationResponse response = configService.createDefaultConfiguration(tenantId);

        // Then
        assertNotNull(response);
        assertEquals(tenantId, response.tenantId());
        verify(configRepository, times(1)).save(any(TenantConfiguration.class));
    }

    @Test
    @DisplayName("Should throw exception when creating duplicate configuration")
    void testCreateDuplicateConfiguration() {
        // Given
        TenantConfiguration existingConfig = new TenantConfiguration();
        existingConfig.assignTenantId(tenantId);

        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(existingConfig));

        // When & Then
        assertThrows(ConflictException.class, () -> configService.createDefaultConfiguration(tenantId));
    }

    @Test
    @DisplayName("Should reject invalid decimal places")
    void testUpdateWithInvalidDecimalPlaces() {
        // Given
        TenantConfiguration config = new TenantConfiguration();
        config.assignTenantId(tenantId);

        TenantConfigurationRequest request = new TenantConfigurationRequest(
                Currency.USD,
                10, // Invalid: > 8
                Language.EN,
                "UTC",
                "yyyy-MM-dd",
                TimeFormat.FORMAT_24H,
                WeekStartDay.MONDAY,
                80,
                "[\"IN_APP\"]",
                "[3,7]",
                "NONE",
                "CALENDAR_MONTH"
        );

        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config));

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> configService.updateConfiguration(request));
    }

    @Test
    @DisplayName("Should reject invalid timezone")
    void testUpdateWithInvalidTimezone() {
        // Given
        TenantConfiguration config = new TenantConfiguration();
        config.assignTenantId(tenantId);

        TenantConfigurationRequest request = new TenantConfigurationRequest(
                Currency.USD,
                2,
                Language.EN,
                "Invalid/Timezone",
                "yyyy-MM-dd",
                TimeFormat.FORMAT_24H,
                WeekStartDay.MONDAY,
                80,
                "[\"IN_APP\"]",
                "[3,7]",
                "NONE",
                "CALENDAR_MONTH"
        );

        when(configRepository.findByTenantId(tenantId)).thenReturn(Optional.of(config));

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> configService.updateConfiguration(request));
    }
}

