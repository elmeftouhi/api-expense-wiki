package com.elmeftouhi.expense.user;

import com.elmeftouhi.expense.security.JwtPrincipal;
import com.elmeftouhi.expense.user.dto.UserCreateRequest;
import com.elmeftouhi.expense.user.dto.UserManagementResponse;
import com.elmeftouhi.expense.user.dto.UserUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping
    public List<UserManagementResponse> listUsers(@AuthenticationPrincipal JwtPrincipal principal) {
        return userManagementService.listUsers(principal.userId());
    }

    @GetMapping("/{userId}")
    public UserManagementResponse getUser(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long userId
    ) {
        return userManagementService.getUser(principal.userId(), userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserManagementResponse createUser(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UserCreateRequest request
    ) {
        return userManagementService.createUser(principal.userId(), request);
    }

    @PutMapping("/{userId}")
    public UserManagementResponse updateUser(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return userManagementService.updateUser(principal.userId(), userId, request);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long userId
    ) {
        userManagementService.deleteUser(principal.userId(), userId);
    }
}

