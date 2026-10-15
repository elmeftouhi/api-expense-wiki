package com.elmeftouhi.expense.auth;

import com.elmeftouhi.expense.auth.dto.AuthResponse;
import com.elmeftouhi.expense.auth.dto.LoginRequest;
import com.elmeftouhi.expense.auth.dto.RegisterRequest;
import com.elmeftouhi.expense.auth.dto.TenantInviteResponse;
import com.elmeftouhi.expense.security.AppUserDetails;
import com.elmeftouhi.expense.security.JwtProperties;
import com.elmeftouhi.expense.security.JwtService;
import com.elmeftouhi.expense.security.TokenBlacklistService;
import com.elmeftouhi.expense.shared.exception.BadRequestException;
import com.elmeftouhi.expense.shared.exception.ConflictException;
import com.elmeftouhi.expense.shared.exception.ResourceNotFoundException;
import com.elmeftouhi.expense.shared.exception.TenantAccessDeniedException;
import com.elmeftouhi.expense.exercice.Exercice;
import com.elmeftouhi.expense.exercice.ExerciceRepository;
import com.elmeftouhi.expense.exercice.ExerciceStatus;
import com.elmeftouhi.expense.tenant.Tenant;
import com.elmeftouhi.expense.tenant.TenantInvite;
import com.elmeftouhi.expense.tenant.TenantInviteRepository;
import com.elmeftouhi.expense.tenant.TenantRepository;
import com.elmeftouhi.expense.user.AppUser;
import com.elmeftouhi.expense.user.AppUserRepository;
import com.elmeftouhi.expense.user.Role;
import com.elmeftouhi.expense.user.TenantRole;
import com.elmeftouhi.expense.user.UserStatus;
import com.elmeftouhi.expense.user.UserTenant;
import com.elmeftouhi.expense.user.UserTenantRepository;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVITE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_LENGTH = 10;
    private static final SecureRandom INVITE_RANDOM = new SecureRandom();

    private final AppUserRepository appUserRepository;
    private final UserTenantRepository userTenantRepository;
    private final TenantRepository tenantRepository;
    private final TenantInviteRepository tenantInviteRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtProperties jwtProperties;
    private final ExerciceRepository exerciceRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (appUserRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ConflictException("Email already exists");
        }

        Tenant tenant = createTenantOrThrow(request.tenantName());

        AppUser user = new AppUser();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName() != null ? request.firstName().trim() : null);
        user.setLastName(request.lastName() != null ? request.lastName().trim() : null);
        user.setPhone(request.phone() != null ? request.phone().trim() : null);
        user.setStatus(UserStatus.ACTIVE);
        user.getRoles().add(Role.ADMIN);
        user.setDefaultTenant(tenant);
        user = appUserRepository.save(user);

        UserTenant userTenant = new UserTenant();
        userTenant.setUser(user);
        userTenant.setTenant(tenant);
        userTenant.setTenantRole(TenantRole.ADMIN);
        userTenantRepository.save(userTenant);

        return buildAuthResponse(user, tenant.getId());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
        AppUser user = appUserRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return buildAuthResponse(user, user.getDefaultTenant().getId());
    }

    @Transactional
    public AuthResponse switchTenant(Long userId, Long tenantId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!userTenantRepository.existsByUserIdAndTenantId(userId, tenantId)) {
            throw new TenantAccessDeniedException("You do not belong to this tenant");
        }

        Tenant selectedTenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        user.setDefaultTenant(selectedTenant);
        appUserRepository.save(user);

        return buildAuthResponse(user, tenantId);
    }

    @Transactional
    public AuthResponse createTenantForUser(Long userId, String tenantName) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Tenant tenant = createTenantOrThrow(tenantName);

        UserTenant userTenant = new UserTenant();
        userTenant.setUser(user);
        userTenant.setTenant(tenant);
        userTenant.setTenantRole(TenantRole.ADMIN);
        userTenantRepository.save(userTenant);

        return buildAuthResponse(user, tenant.getId());
    }

    @Transactional
    public AuthResponse joinTenantForUser(Long userId, String inviteCode) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TenantInvite invite = tenantInviteRepository.findByCodeIgnoreCase(inviteCode.trim())
                .orElseThrow(() -> new BadRequestException("Invalid or expired invite code"));

        Long tenantId = invite.getTenant().getId();
        Tenant tenant = invite.getTenant();

        if (userTenantRepository.existsByUserIdAndTenantId(userId, tenantId)) {
            throw new ConflictException("You already belong to this tenant");
        }

        Instant now = Instant.now();
        int consumed = tenantInviteRepository.consumeInviteIfActive(invite.getId(), now, now);
        if (consumed == 0) {
            throw new BadRequestException("Invalid or expired invite code");
        }

        UserTenant userTenant = new UserTenant();
        userTenant.setUser(user);
        userTenant.setTenant(tenant);
        userTenant.setTenantRole(invite.getInvitedRole() != null ? invite.getInvitedRole() : TenantRole.VIEWER);
        try {
            userTenantRepository.save(userTenant);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("You already belong to this tenant");
        }

        return buildAuthResponse(user, tenantId);
    }

    @Transactional
    public TenantInviteResponse createTenantInviteForUser(Long userId, Long tenantId, Integer expiresInHours, TenantRole role) {
        AppUser user = loadUserWithDefaultTenant(userId);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        boolean isGlobalAdmin = user.getRoles().contains(Role.ADMIN);
        boolean isTenantAdmin = isGlobalAdmin || hasTenantRole(userId, tenantId, TenantRole.ADMIN);
        if (!isTenantAdmin) {
            throw new TenantAccessDeniedException("Only tenant admins can create invites");
        }

        Instant expiresAt = Instant.now().plusSeconds(expiresInHours.longValue() * 3600);

        TenantInvite invite = new TenantInvite();
        invite.setCode(generateUniqueInviteCode());
        invite.setTenant(tenant);
        invite.setCreatedBy(user);
        invite.setExpiresAt(expiresAt);
        invite.setInvitedRole(role != null ? role : TenantRole.VIEWER);
        invite = tenantInviteRepository.save(invite);

        return new TenantInviteResponse(invite.getCode(), tenantId, invite.getExpiresAt(), invite.getInvitedRole());
    }

    @Transactional(readOnly = true)
    public void logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        tokenBlacklistService.revoke(token, jwtService.extractExpiration(token));
    }

    @Transactional
    public void removeTenantMember(Long callerUserId, Long tenantId, Long memberUserId) {
        AppUser caller = loadUserWithDefaultTenant(callerUserId);

        boolean isGlobalAdmin = caller.getRoles().contains(Role.ADMIN);
        boolean isTenantAdmin = isGlobalAdmin || hasTenantRole(callerUserId, tenantId, TenantRole.ADMIN);
        if (!isTenantAdmin) {
            throw new TenantAccessDeniedException("Only tenant admins can remove members");
        }

        if (!isGlobalAdmin && !userTenantRepository.existsByUserIdAndTenantId(callerUserId, tenantId)) {
            throw new TenantAccessDeniedException("You do not belong to this tenant");
        }

        if (callerUserId.equals(memberUserId)) {
            throw new BadRequestException("Self-removal is not allowed from this endpoint");
        }

        AppUser member = loadUserWithDefaultTenant(memberUserId);

        userTenantRepository.findByUserIdAndTenantId(memberUserId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in this tenant"));

        if (member.getDefaultTenant().getId().equals(tenantId)) {
            throw new ConflictException("Cannot remove tenant owner. Transfer ownership first");
        }

        long deleted = userTenantRepository.deleteByUserIdAndTenantId(memberUserId, tenantId);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Member not found in this tenant");
        }
    }

    private AuthResponse buildAuthResponse(AppUser user, Long selectedTenantId) {
        Set<Long> allowedTenantIds = userTenantRepository.findAllByUserId(user.getId())
                .stream()
                .map(userTenant -> userTenant.getTenant().getId())
                .collect(Collectors.toCollection(HashSet::new));
        allowedTenantIds.add(user.getDefaultTenant().getId());

        String token = jwtService.generateToken(user, selectedTenantId, allowedTenantIds);
        return new AuthResponse(token, "Bearer", jwtProperties.getExpirationMinutes(), selectedTenantId, allowedTenantIds);
    }

    private Tenant createTenantOrThrow(String tenantName) {
        String normalizedTenantName = tenantName.trim();
        if (tenantRepository.existsByNameIgnoreCase(normalizedTenantName)) {
            throw new ConflictException("Tenant name already exists");
        }

        Tenant tenant = new Tenant();
        tenant.setName(normalizedTenantName);
        Tenant saved = tenantRepository.save(tenant);
        try {
            TenantContext.setTenantId(saved.getId());
            initializeCurrentYearExercice(saved.getId());
        } finally {
            TenantContext.clear();
        }
        return saved;
    }

    private void initializeCurrentYearExercice(Long tenantId) {
        LocalDate today = LocalDate.now();
        if (exerciceRepository.findExerciceForDate(tenantId, today).isPresent()) {
            return;
        }

        int year = today.getYear();
        Exercice exercice = new Exercice();
        exercice.setName("Exercice " + year);
        exercice.setStartDate(LocalDate.of(year, 1, 1));
        exercice.setEndDate(LocalDate.of(year, 12, 31));
        exercice.setStatus(ExerciceStatus.OPEN);
        exerciceRepository.save(exercice);
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new BadRequestException("Missing or invalid Authorization header");
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new BadRequestException("Missing bearer token");
        }
        return token;
    }

    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder codeBuilder = new StringBuilder(INVITE_CODE_LENGTH);
            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                int index = INVITE_RANDOM.nextInt(INVITE_CODE_ALPHABET.length());
                codeBuilder.append(INVITE_CODE_ALPHABET.charAt(index));
            }
            String code = codeBuilder.toString();
            if (!tenantInviteRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to generate unique invite code");
    }

    private AppUser loadUserWithDefaultTenant(Long userId) {
        return appUserRepository.findWithDefaultTenantById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private boolean hasTenantRole(Long userId, Long tenantId, TenantRole requiredRole) {
        return userTenantRepository.findByUserIdAndTenantId(userId, tenantId)
                .map(this::resolveTenantRole)
                .map(role -> role.atLeast(requiredRole))
                .orElse(false);
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
}
