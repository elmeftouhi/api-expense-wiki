package com.elmeftouhi.expense.datatransfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elmeftouhi.expense.activity.repository.ActivityLogRepository;
import com.elmeftouhi.expense.budget.repository.BudgetPeriodInstanceRepository;
import com.elmeftouhi.expense.budget.repository.BudgetRepository;
import com.elmeftouhi.expense.category.entity.Category;
import com.elmeftouhi.expense.category.repository.CategoryRepository;
import com.elmeftouhi.expense.datatransfer.dto.ImportUserDataRequest;
import com.elmeftouhi.expense.datatransfer.dto.ImportUserDataResponse;
import com.elmeftouhi.expense.datatransfer.dto.UserDataBackupSnapshot;
import com.elmeftouhi.expense.datatransfer.service.UserDataTransferService;
import com.elmeftouhi.expense.expense.ExpenseStatus;
import com.elmeftouhi.expense.expense.ExpenseType;
import com.elmeftouhi.expense.expense.entity.Expense;
import com.elmeftouhi.expense.expense.repository.ExpenseRepository;
import com.elmeftouhi.expense.expense.repository.SubscriptionPaymentRepository;
import com.elmeftouhi.expense.expense.repository.SubscriptionRepository;
import com.elmeftouhi.expense.exercice.repository.ExerciceRepository;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.notification.repository.NotificationRepository;
import com.elmeftouhi.expense.paymentmethod.repository.PaymentMethodRepository;
import com.elmeftouhi.expense.security.JwtPrincipal;
import com.elmeftouhi.expense.security.TenantPermissionService;
import com.elmeftouhi.expense.shared.exception.ConflictException;
import com.elmeftouhi.expense.tenant.entity.Tenant;
import com.elmeftouhi.expense.tenant.repository.TenantRepository;
import com.elmeftouhi.expense.tenantconfig.repository.TenantConfigurationRepository;
import com.elmeftouhi.expense.tenantconfig.service.TenantConfigurationService;
import com.elmeftouhi.expense.user.TenantRole;
import com.elmeftouhi.expense.user.entity.AppUser;
import com.elmeftouhi.expense.user.entity.UserTenant;
import com.elmeftouhi.expense.user.repository.AppUserRepository;
import com.elmeftouhi.expense.user.repository.UserTenantRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class UserDataTransferServiceTests {

    @Mock
    private TenantPermissionService tenantPermissionService;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private UserTenantRepository userTenantRepository;
    @Mock
    private ExerciceRepository exerciceRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private BudgetPeriodInstanceRepository budgetPeriodInstanceRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private SubscriptionPaymentRepository subscriptionPaymentRepository;
    @Mock
    private TenantConfigurationRepository tenantConfigurationRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private ActivityLogRepository activityLogRepository;
    @Mock
    private TenantConfigurationService tenantConfigurationService;

    @InjectMocks
    private UserDataTransferService userDataTransferService;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void exportCurrentTenantData_requiresAdminAndBuildsMetadata() {
        setPrincipal(10L, "owner@example.com", 70L);
        TenantContext.setTenantId(70L);

        Tenant tenant = new Tenant();
        tenant.setId(70L);
        tenant.setName("My Workspace");

        when(tenantRepository.findById(70L)).thenReturn(Optional.of(tenant));
        when(exerciceRepository.findAllByTenantIdOrderByStartDateDesc(70L)).thenReturn(List.of());
        when(categoryRepository.findByTenantIdOrderByLevelAscNameAscIdAsc(70L)).thenReturn(List.of());
        when(paymentMethodRepository.findByTenantIdOrderByIsDefaultDescNameAsc(70L)).thenReturn(List.of());
        when(budgetRepository.findByTenantIdOrderByCreatedAtDesc(70L)).thenReturn(List.of());
        when(budgetPeriodInstanceRepository.findByTenantIdOrderByStartDateAscIdAsc(70L)).thenReturn(List.of());
        when(subscriptionRepository.findByTenantIdOrderByCreatedAtAscIdAsc(70L)).thenReturn(List.of());
        when(expenseRepository.findByTenantIdOrderByDueDateDesc(70L)).thenReturn(List.of());
        when(subscriptionPaymentRepository.findByTenantIdOrderByScheduledBillingDateAscIdAsc(70L)).thenReturn(List.of());
        when(notificationRepository.findByTenantIdOrderByCreatedAtAscIdAsc(70L)).thenReturn(List.of());
        when(activityLogRepository.findByTenantIdOrderByOccurredAtAscIdAsc(70L)).thenReturn(List.of());

        UserDataBackupSnapshot snapshot = userDataTransferService.exportCurrentTenantData();

        verify(tenantPermissionService).requireAdmin();
        assertEquals("1.0", snapshot.metadata().formatVersion());
        assertEquals(70L, snapshot.metadata().sourceTenantId());
        assertEquals("owner@example.com", snapshot.metadata().exportedByUsername());
    }

    @Test
    void importIntoNewTenant_createsMembershipAndRemapsExpenseUser() {
        setPrincipal(11L, "importer@example.com", 5L);
        TenantContext.setTenantId(5L);

        Tenant existingDefaultTenant = new Tenant();
        existingDefaultTenant.setId(5L);
        existingDefaultTenant.setName("old-tenant");

        AppUser importer = new AppUser();
        importer.setId(11L);
        importer.setEmail("importer@example.com");
        importer.setDefaultTenant(existingDefaultTenant);

        Tenant createdTenant = new Tenant();
        createdTenant.setId(90L);
        createdTenant.setName("Imported tenant");

        when(appUserRepository.findWithDefaultTenantById(11L)).thenReturn(Optional.of(importer));
        when(tenantRepository.existsByNameIgnoreCase("Imported tenant")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(createdTenant);

        Category importedCategory = new Category();
        importedCategory.setId(700L);
        when(categoryRepository.save(any(Category.class))).thenReturn(importedCategory);

        Expense importedExpense = new Expense();
        importedExpense.setId(800L);
        when(expenseRepository.save(any(Expense.class))).thenReturn(importedExpense);

        when(exerciceRepository.findExerciceForDate(90L, LocalDate.now())).thenReturn(Optional.of(new com.elmeftouhi.expense.exercice.entity.Exercice()));

        UserDataBackupSnapshot backup = new UserDataBackupSnapshot(
                new UserDataBackupSnapshot.BackupMetadata("1.0", Instant.now(), 5L, "source", 11L, "importer@example.com"),
                null,
                List.of(),
                List.of(new UserDataBackupSnapshot.CategoryEntry(
                        1L,
                        null,
                        0,
                        "Food",
                        "",
                        null,
                        null,
                        null,
                        null,
                        true,
                        false)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new UserDataBackupSnapshot.ExpenseEntry(
                        2L,
                        1L,
                        999L,
                        "Lunch",
                        BigDecimal.valueOf(12.5),
                        "USD",
                        ExpenseType.NORMAL,
                        ExpenseStatus.PENDING,
                        LocalDate.now(),
                        null,
                        null,
                        null,
                        false,
                        Instant.now(),
                        Instant.now(),
                        null)),
                List.of(),
                List.of(),
                List.of());

        ImportUserDataResponse response = userDataTransferService.importIntoNewTenant(
                new ImportUserDataRequest("Imported tenant", backup, false));

        assertEquals(90L, response.tenantId());
        assertEquals(1, response.counts().categories());
        assertEquals(1, response.counts().expenses());

        ArgumentCaptor<UserTenant> membershipCaptor = ArgumentCaptor.forClass(UserTenant.class);
        verify(userTenantRepository).save(membershipCaptor.capture());
        assertEquals(TenantRole.ADMIN, membershipCaptor.getValue().getTenantRole());

        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(expenseCaptor.capture());
        assertEquals(11L, expenseCaptor.getValue().getUserId());

        assertEquals(5L, TenantContext.getTenantId());
    }

    @Test
    void importIntoNewTenant_rejectsDuplicateTenantName() {
        setPrincipal(15L, "importer@example.com", 1L);

        AppUser importer = new AppUser();
        importer.setId(15L);
        importer.setDefaultTenant(new Tenant());

        when(appUserRepository.findWithDefaultTenantById(15L)).thenReturn(Optional.of(importer));
        when(tenantRepository.existsByNameIgnoreCase("Taken")).thenReturn(true);

        UserDataBackupSnapshot backup = new UserDataBackupSnapshot(
                new UserDataBackupSnapshot.BackupMetadata("1.0", Instant.now(), 1L, "source", 15L, "importer@example.com"),
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());

        assertThrows(
                ConflictException.class,
                () -> userDataTransferService.importIntoNewTenant(new ImportUserDataRequest("Taken", backup, false)));
    }

    private void setPrincipal(Long userId, String username, Long tenantId) {
        JwtPrincipal principal = new JwtPrincipal(userId, username, tenantId, Set.of(tenantId));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "n/a", List.of()));
    }
}

