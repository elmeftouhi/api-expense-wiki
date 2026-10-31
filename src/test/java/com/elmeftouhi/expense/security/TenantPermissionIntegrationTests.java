package com.elmeftouhi.expense.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.elmeftouhi.expense.auth.dto.RegisterRequest;
import com.elmeftouhi.expense.auth.service.AuthService;
import com.elmeftouhi.expense.budget.BudgetPeriodicity;
import com.elmeftouhi.expense.budget.BudgetWarningType;
import com.elmeftouhi.expense.budget.dto.BudgetCreateRequest;
import com.elmeftouhi.expense.budget.dto.BudgetPeriodUpdateRequest;
import com.elmeftouhi.expense.budget.service.BudgetService;
import com.elmeftouhi.expense.category.dto.CategoryCreateRequest;
import com.elmeftouhi.expense.category.service.CategoryService;
import com.elmeftouhi.expense.exercice.ExerciceStatus;
import com.elmeftouhi.expense.exercice.dto.ExerciceCreateRequest;
import com.elmeftouhi.expense.exercice.service.ExerciceService;
import com.elmeftouhi.expense.expense.ExpenseStatus;
import com.elmeftouhi.expense.expense.ExpenseType;
import com.elmeftouhi.expense.expense.SubscriptionFrequency;
import com.elmeftouhi.expense.expense.SubscriptionLifecycleStatus;
import com.elmeftouhi.expense.expense.dto.ExpenseCreateRequest;
import com.elmeftouhi.expense.expense.dto.SubscriptionCreateRequest;
import com.elmeftouhi.expense.expense.dto.SubscriptionPaymentBackfillRequest;
import com.elmeftouhi.expense.expense.service.ExpenseService;
import com.elmeftouhi.expense.expense.service.SubscriptionGenerationService;
import com.elmeftouhi.expense.expense.service.SubscriptionPaymentService;
import com.elmeftouhi.expense.expense.service.SubscriptionService;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.paymentmethod.PaymentMethodType;
import com.elmeftouhi.expense.paymentmethod.dto.PaymentMethodRequest;
import com.elmeftouhi.expense.paymentmethod.service.PaymentMethodService;
import com.elmeftouhi.expense.shared.exception.ResourceNotFoundException;
import com.elmeftouhi.expense.shared.exception.TenantAccessDeniedException;
import com.elmeftouhi.expense.tenantconfig.ConfigurationContext;
import com.elmeftouhi.expense.tenantconfig.dto.TenantConfigurationRequest;
import com.elmeftouhi.expense.tenantconfig.service.TenantConfigurationService;
import com.elmeftouhi.expense.user.Role;
import com.elmeftouhi.expense.user.TenantRole;
import com.elmeftouhi.expense.user.entity.AppUser;
import com.elmeftouhi.expense.user.repository.AppUserRepository;
import com.elmeftouhi.expense.user.repository.UserTenantRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TenantPermissionIntegrationTests {

    @Autowired private AuthService authService;
    @Autowired private AppUserRepository users;
    @Autowired private UserTenantRepository memberships;
    @Autowired private TenantPermissionService permissions;
    @Autowired private CategoryService categories;
    @Autowired private ExpenseService expenses;
    @Autowired private SubscriptionService subscriptions;
    @Autowired private SubscriptionPaymentService payments;
    @Autowired private SubscriptionGenerationService generation;
    @Autowired private BudgetService budgets;
    @Autowired private PaymentMethodService paymentMethods;
    @Autowired private ExerciceService exercices;
    @Autowired private TenantConfigurationService configuration;
    @Autowired private JwtService jwtService;
    @Autowired private MockMvc mvc;

    private AppUser user;
    private Long tenantId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        ConfigurationContext.clear();
        user = registerUser();
        tenantId = user.getDefaultTenant().getId();
        authenticate(user, tenantId);
    }

    @AfterEach
    void tearDown() {
        configuration.invalidateCache(tenantId);
        ConfigurationContext.clear();
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void viewerCannotUseAnyFinancialOrConfigurationMutation() {
        setRole(TenantRole.VIEWER);
        assertAllWritesDenied();
    }

    @Test
    void missingAuthenticationDoesNotBypassPermissionsForBackgroundJobs() {
        SecurityContextHolder.clearContext();
        assertAllWritesDenied();
    }

    @Test
    void globalAdminWithViewerMembershipCannotBypassTenantPermissions() {
        user.getRoles().add(Role.ADMIN);
        users.saveAndFlush(user);
        setRole(TenantRole.VIEWER);
        assertAllWritesDenied();
    }

    @Test
    void missingMembershipCannotBypassPermissionsEvenWithGlobalAdminRole() {
        user.getRoles().add(Role.ADMIN);
        users.saveAndFlush(user);
        memberships.deleteByUserIdAndTenantId(user.getId(), tenantId);
        memberships.flush();
        assertAllWritesDenied();
    }

    @Test
    void nullMembershipRoleDoesNotInferAdminFromDefaultTenant() {
        setRole(null);
        assertAllWritesDenied();
    }

    @Test
    void mismatchedSelectedTenantAndMissingTenantContextFailClosed() {
        TenantContext.setTenantId(-1L);
        assertThrows(TenantAccessDeniedException.class, permissions::requireEditor);
        TenantContext.clear();
        assertThrows(TenantAccessDeniedException.class, permissions::requireEditor);
    }

    @Test
    void unauthenticatedTokenAndNonJwtPrincipalFailClosed() {
        JwtPrincipal principal = new JwtPrincipal(user.getId(), user.getEmail(), tenantId, Set.of(tenantId));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null));
        assertThrows(TenantAccessDeniedException.class, permissions::requireEditor);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));
        assertThrows(TenantAccessDeniedException.class, permissions::requireEditor);
    }

    @Test
    void databaseRoleChangesApplyWithoutIssuingAnotherToken() {
        setRole(TenantRole.EDITOR);
        permissions.requireEditor();
        setRole(TenantRole.VIEWER);
        assertThrows(TenantAccessDeniedException.class, permissions::requireEditor);
        setRole(TenantRole.ADMIN);
        permissions.requireAdmin();
    }

    @ParameterizedTest
    @EnumSource(TenantRole.class)
    void allTenantRolesCanReadFinancialData(TenantRole role) {
        var category = categories.create(categoryRequest("Readable"));
        setRole(role);
        assertThat(categories.getById(category.id()).name()).isEqualTo("Readable");
        assertThat(categories.findAll()).hasSize(1);
        assertThat(budgets.findAll()).isEmpty();
        assertThat(expenses.findOverdue()).isEmpty();
        assertThat(paymentMethods.findAll(false)).isEmpty();
        assertThat(exercices.findAll()).isNotEmpty();
        assertThat(configuration.getConfiguration().tenantId()).isEqualTo(tenantId);
    }

    @ParameterizedTest
    @EnumSource(value = TenantRole.class, names = {"EDITOR", "ADMIN"})
    void editorsAndAdminsCanPerformFinancialWrites(TenantRole role) {
        setRole(role);
        var category = categories.create(categoryRequest("Financial"));
        categories.update(category.id(), categoryRequest("Updated"));
        categories.updateLevel(category.id(), 1);
        categories.switchStatus(category.id());
        categories.switchStatus(category.id());

        var method = paymentMethods.create(new PaymentMethodRequest("Cash", PaymentMethodType.CASH, true, true));
        paymentMethods.update(method.id(), new PaymentMethodRequest("Wallet", PaymentMethodType.CASH, true, true));
        paymentMethods.setDefault(method.id());

        var expenseRequest = new ExpenseCreateRequest(category.id(), "Lunch", BigDecimal.TEN,
                LocalDate.now(), null, ExpenseType.NORMAL, method.id(), null);
        var expense = expenses.create(expenseRequest);
        expenses.update(expense.id(), expenseRequest);
        assertThat(expenses.markAsPaid(expense.id(), LocalDate.now()).status()).isEqualTo(ExpenseStatus.PAID);
        expenses.changeStatus(expense.id(), ExpenseStatus.PENDING, null);
        expenses.delete(expense.id());

        var subscriptionRequest = subscriptionRequest(category.id(), method.id());
        var subscription = subscriptions.create(subscriptionRequest);
        subscriptions.update(subscription.id(), subscriptionRequest);
        subscriptions.pause(subscription.id());
        subscriptions.resume(subscription.id(), LocalDate.now());
        assertThat(payments.generateNextPayment(subscription.id()).generated()).isTrue();
        assertThat(payments.backfillPayment(subscription.id(), new SubscriptionPaymentBackfillRequest(
                LocalDate.now().minusMonths(1), LocalDate.now().minusMonths(1))).generated()).isTrue();
        assertThat(subscriptions.stop(subscription.id()).status()).isEqualTo(SubscriptionLifecycleStatus.STOPPED);

        var budgetRequest = new BudgetCreateRequest("Monthly", null, BudgetPeriodicity.MONTHLY,
                new BigDecimal("1000"), BudgetWarningType.PERCENTAGE, new BigDecimal("80"), Set.of(category.id()), true);
        var budget = budgets.create(budgetRequest);
        budgets.update(budget.id(), budgetRequest);
        var period = budgets.getPeriods(budget.id()).get(0);
        budgets.updatePeriod(budget.id(), period.id(), new BudgetPeriodUpdateRequest(
                new BigDecimal("1200"), BudgetWarningType.PERCENTAGE, new BigDecimal("80")));

        int nextYear = LocalDate.now().getYear() + 1;
        var exercice = exercices.create(new ExerciceCreateRequest("Next year",
                LocalDate.of(nextYear, 1, 1), LocalDate.of(nextYear, 12, 31)));
        exercices.updateStatus(exercice.id(), ExerciceStatus.CLOSED);
        var disposableCategory = categories.create(categoryRequest("Disposable"));
        categories.delete(disposableCategory.id());
    }

    @ParameterizedTest
    @EnumSource(value = TenantRole.class, names = {"VIEWER", "EDITOR"})
    void nonAdminsCannotManageSettingsInvitationsOrMembers(TenantRole role) {
        setRole(role);
        assertThrows(TenantAccessDeniedException.class, () -> configuration.updateConfiguration(null));
        assertThrows(TenantAccessDeniedException.class,
                () -> authService.createTenantInviteForUser(user.getId(), tenantId, 2, TenantRole.ADMIN));
        assertThrows(TenantAccessDeniedException.class,
                () -> authService.updateTenantForUser(user.getId(), tenantId, "Forbidden"));
        assertThrows(TenantAccessDeniedException.class,
                () -> authService.removeTenantMember(user.getId(), tenantId, -1L));
    }

    @Test
    void tenantAdminCanManageSettingsInvitationsAndMembers() {
        var request = new TenantConfigurationRequest(null, 3, null, null, null, null,
                null, null, null, null, null, null);
        assertThat(configuration.updateConfiguration(request).decimalPlaces()).isEqualTo(3);
        AppUser member = registerUser();
        String code = authService.createTenantInviteForUser(user.getId(), tenantId, 2, TenantRole.VIEWER).inviteCode();
        authService.joinTenantForUser(member.getId(), code);
        authService.updateTenantForUser(user.getId(), tenantId, "Renamed-" + tenantId);
        authService.removeTenantMember(user.getId(), tenantId, member.getId());
        assertThat(memberships.existsByUserIdAndTenantId(member.getId(), tenantId)).isFalse();
    }

    @Test
    void crossTenantAdminOperationsAreDeniedEvenForSystemAdmins() {
        AppUser other = registerUser();
        Long otherTenantId = other.getDefaultTenant().getId();
        user.getRoles().add(Role.ADMIN);
        users.saveAndFlush(user);
        assertThrows(TenantAccessDeniedException.class,
                () -> authService.createTenantInviteForUser(user.getId(), otherTenantId, 2, TenantRole.ADMIN));
        assertThrows(TenantAccessDeniedException.class,
                () -> authService.updateTenantForUser(user.getId(), otherTenantId, "Forbidden"));
        assertThrows(TenantAccessDeniedException.class,
                () -> authService.removeTenantMember(user.getId(), otherTenantId, other.getId()));
    }

    @Test
    void editorCannotModifyRecordsFromAnotherTenant() {
        AppUser other = registerUser();
        authenticate(other, other.getDefaultTenant().getId());
        var foreignCategory = categories.create(categoryRequest("Foreign"));
        authenticate(user, tenantId);
        setRole(TenantRole.EDITOR);
        assertThrows(ResourceNotFoundException.class,
                () -> categories.update(foreignCategory.id(), categoryRequest("Forbidden")));
    }

    @Test
    void scheduledGenerationWorksWithoutAnAuthenticatedUserAndRemainsIdempotent() {
        var category = categories.create(categoryRequest("Scheduled"));
        var subscription = subscriptions.create(subscriptionRequest(category.id(), null));
        SecurityContextHolder.clearContext();
        assertThat(generation.generateDueOccurrence(tenantId, subscription.id(), LocalDate.now())).isTrue();
        assertThat(generation.generateDueOccurrence(tenantId, subscription.id(), LocalDate.now())).isFalse();
    }

    @Test
    void httpReturnsForbiddenForViewerWritesButAllowsReadsAndEditorWrites() throws Exception {
        String token = jwtService.generateToken(user, tenantId, Set.of(tenantId));
        setRole(TenantRole.VIEWER);
        mvc.perform(get("/v1/categories").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        String json = "{\"name\":\"HTTP category\",\"active\":true,\"allowOverBudget\":false}";
        mvc.perform(post("/v1/categories").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isForbidden());
        setRole(TenantRole.EDITOR);
        mvc.perform(post("/v1/categories").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isCreated());
        mvc.perform(put("/v1/config").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        setRole(TenantRole.ADMIN);
        mvc.perform(put("/v1/config").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
    }

    private void assertAllWritesDenied() {
        // Invalid payloads/IDs deliberately prove authorization runs before validation or data access.
        Stream<Mutation> mutations = Stream.of(
                new Mutation("expense create", () -> expenses.create(null)),
                new Mutation("expense update", () -> expenses.update(-1L, null)),
                new Mutation("expense delete", () -> expenses.delete(-1L)),
                new Mutation("expense paid", () -> expenses.markAsPaid(-1L, LocalDate.now())),
                new Mutation("expense status", () -> expenses.changeStatus(-1L, ExpenseStatus.PAID, LocalDate.now())),
                new Mutation("subscription create", () -> subscriptions.create(null)),
                new Mutation("subscription update", () -> subscriptions.update(-1L, null)),
                new Mutation("subscription pause", () -> subscriptions.pause(-1L)),
                new Mutation("subscription resume", () -> subscriptions.resume(-1L, LocalDate.now())),
                new Mutation("subscription stop", () -> subscriptions.stop(-1L)),
                new Mutation("payment generate", () -> payments.generateNextPayment(-1L)),
                new Mutation("payment backfill", () -> payments.backfillPayment(-1L, null)),
                new Mutation("category create", () -> categories.create(null)),
                new Mutation("category update", () -> categories.update(-1L, null)),
                new Mutation("category delete", () -> categories.delete(-1L)),
                new Mutation("category status", () -> categories.switchStatus(-1L)),
                new Mutation("category order", () -> categories.updateLevel(-1L, 1)),
                new Mutation("budget create", () -> budgets.create(null)),
                new Mutation("budget update", () -> budgets.update(-1L, null)),
                new Mutation("budget period", () -> budgets.updatePeriod(-1L, -1L, null)),
                new Mutation("payment method create", () -> paymentMethods.create(null)),
                new Mutation("payment method update", () -> paymentMethods.update(-1L, null)),
                new Mutation("payment method default", () -> paymentMethods.setDefault(-1L)),
                new Mutation("exercice create", () -> exercices.create(null)),
                new Mutation("exercice status", () -> exercices.updateStatus(-1L, ExerciceStatus.CLOSED)),
                new Mutation("configuration update", () -> configuration.updateConfiguration(null)));
        assertAll(mutations.map(mutation -> () -> assertThrows(
                TenantAccessDeniedException.class, mutation.action(), mutation.name())));
    }

    private AppUser registerUser() {
        String unique = "permissions-" + System.nanoTime();
        authService.register(new RegisterRequest(unique + "@example.com", "Password123!", unique, null, null, null));
        return users.findByEmail(unique + "@example.com").orElseThrow();
    }

    private void authenticate(AppUser account, Long selectedTenantId) {
        TenantContext.setTenantId(selectedTenantId);
        ConfigurationContext.setConfiguration(configuration.loadConfiguration(selectedTenantId));
        var principal = new JwtPrincipal(account.getId(), account.getEmail(), selectedTenantId, Set.of(selectedTenantId));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private void setRole(TenantRole role) {
        var membership = memberships.findByUserIdAndTenantId(user.getId(), tenantId).orElseThrow();
        membership.setTenantRole(role);
        memberships.saveAndFlush(membership);
    }

    private CategoryCreateRequest categoryRequest(String name) {
        return new CategoryCreateRequest(null, name, null, null, null, null, null, 1, true, true);
    }

    private SubscriptionCreateRequest subscriptionRequest(Long categoryId, Long paymentMethodId) {
        return new SubscriptionCreateRequest("Hosting", null, categoryId, BigDecimal.TEN, null,
                SubscriptionFrequency.MONTHLY, null, LocalDate.now(), null, 5, paymentMethodId, null);
    }

    private record Mutation(String name, Executable action) {
    }
}
