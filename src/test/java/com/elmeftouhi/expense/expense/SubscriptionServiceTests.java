package com.elmeftouhi.expense.expense;

import com.elmeftouhi.expense.expense.entity.Subscription;
import com.elmeftouhi.expense.expense.repository.SubscriptionRepository;
import com.elmeftouhi.expense.expense.service.SubscriptionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elmeftouhi.expense.activity.service.ActivityLogService;
import com.elmeftouhi.expense.category.entity.Category;
import com.elmeftouhi.expense.category.repository.CategoryRepository;
import com.elmeftouhi.expense.expense.dto.SubscriptionCreateRequest;
import com.elmeftouhi.expense.expense.dto.SubscriptionResponse;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.paymentmethod.entity.PaymentMethod;
import com.elmeftouhi.expense.paymentmethod.service.PaymentMethodService;
import com.elmeftouhi.expense.security.JwtPrincipal;
import com.elmeftouhi.expense.shared.exception.BadRequestException;
import com.elmeftouhi.expense.tenantconfig.ConfigurationContext;
import com.elmeftouhi.expense.tenantconfig.Currency;
import com.elmeftouhi.expense.tenantconfig.entity.TenantConfiguration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTests {

    @Mock
    private SubscriptionRepository subscriptionRepository;


    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private PaymentMethodService paymentMethodService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private final Long tenantId = 1L;
    private final Long userId = 99L;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(tenantId);
        TenantConfiguration config = new TenantConfiguration();
        config.setCurrency(Currency.USD);
        config.setTimezone("UTC");
        ConfigurationContext.setConfiguration(config);

        JwtPrincipal principal = new JwtPrincipal(userId, "test-user", tenantId, Set.of(tenantId));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null)
        );
    }

    @Test
    void createSubscription_independentFromExpense() {
        Category category = new Category();
        category.setId(7L);
        category.setName("Utilities");

        SubscriptionCreateRequest request = new SubscriptionCreateRequest(
                "Hosting",
                "VPS",
                7L,
                BigDecimal.valueOf(120),
                "USD",
                SubscriptionFrequency.ANNUAL,
                null,
                LocalDate.of(2026, 1, 10),
                null,
                5,
                3L,
                "auto-renew"
        );

        Subscription saved = new Subscription();
        saved.setId(10L);
        saved.assignTenantId(tenantId);
        saved.setName("Hosting");
        saved.setDescription("VPS");
        saved.setCategory(category);
        saved.setOwnerUserId(userId);
        saved.setAmount(BigDecimal.valueOf(120));
        saved.setCurrency("USD");
        saved.setFrequency(SubscriptionFrequency.ANNUAL);
        saved.setNextBillingDate(LocalDate.of(2026, 1, 10));
        saved.setStatus(SubscriptionLifecycleStatus.ACTIVE);
        saved.setReminderLeadDays(5);
        saved.setAnchorDayOfMonth(10);
        saved.setAnchorMonthOfYear(1);

        when(categoryRepository.findByIdAndTenantId(7L, tenantId)).thenReturn(Optional.of(category));
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setId(3L);
        paymentMethod.setName("Cash");
        paymentMethod.setActive(true);
        when(paymentMethodService.resolveOptionalForUsage(tenantId, 3L)).thenReturn(paymentMethod);
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(saved);

        SubscriptionResponse response = subscriptionService.create(request);

        assertEquals(10L, response.id());
        assertEquals("Hosting", response.name());
        assertEquals(SubscriptionLifecycleStatus.ACTIVE, response.status());
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void resumeStoppedSubscription_isRejected() {
        Subscription stopped = new Subscription();
        stopped.setId(5L);
        stopped.assignTenantId(tenantId);
        stopped.setStatus(SubscriptionLifecycleStatus.STOPPED);

        when(subscriptionRepository.findByIdAndTenantId(5L, tenantId)).thenReturn(Optional.of(stopped));

        assertThrows(BadRequestException.class,
                () -> subscriptionService.resume(5L, LocalDate.of(2026, 12, 1)));
    }

    @Test
    void resumeRequiresExplicitNextBillingDate() {
        Subscription paused = new Subscription();
        paused.setId(6L);
        paused.assignTenantId(tenantId);
        paused.setStatus(SubscriptionLifecycleStatus.PAUSED);

        when(subscriptionRepository.findByIdAndTenantId(6L, tenantId)).thenReturn(Optional.of(paused));

        assertThrows(BadRequestException.class, () -> subscriptionService.resume(6L, null));
    }

    @Test
    void searchByCategoryIds_filtersByProvidedIdsOnly() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Subscription> page = new PageImpl<>(List.of(), pageable, 0);

        when(subscriptionRepository.searchWithoutText(tenantId, null, true, List.of(10L, 11L, 12L), null, null, null, pageable))
                .thenReturn(page);

        subscriptionService.search(null, null, List.of(10L, 11L, 12L), null, null, null, pageable);

        verify(subscriptionRepository).searchWithoutText(
                eq(tenantId),
                eq(null),
                eq(true),
                eq(List.of(10L, 11L, 12L)),
                eq(null),
                eq(null),
                eq(null),
                eq(pageable));
    }

    @Test
    void searchWithNoCategoryIds_doesNotApplyCategoryFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Subscription> page = new PageImpl<>(List.of(), pageable, 0);

        when(subscriptionRepository.searchWithoutText(tenantId, null, false, null, null, null, null, pageable))
                .thenReturn(page);

        subscriptionService.search(null, null, null, null, null, null, pageable);

        verify(subscriptionRepository).searchWithoutText(
                eq(tenantId),
                eq(null),
                eq(false),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(pageable));
    }
}

