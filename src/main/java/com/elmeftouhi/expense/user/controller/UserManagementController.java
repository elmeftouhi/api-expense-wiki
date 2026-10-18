package com.elmeftouhi.expense.user.controller;

import com.elmeftouhi.expense.user.service.UserManagementService;
import com.elmeftouhi.expense.security.JwtPrincipal;
import com.elmeftouhi.expense.user.dto.UserCreateRequest;
import com.elmeftouhi.expense.user.dto.UserManagementResponse;
import com.elmeftouhi.expense.user.dto.UserUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "User Management", description = "Manage tenant users: list, read, create, update, and delete")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping
    @Operation(summary = "List users", description = "Returns all users visible to the authenticated user in the current tenant")
    @ApiResponse(responseCode = "200", description = "Users returned successfully")
    public List<UserManagementResponse> listUsers(@AuthenticationPrincipal JwtPrincipal principal) {
        return userManagementService.listUsers(principal.userId());
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Returns details for a single user within the tenant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User returned successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public UserManagementResponse getUser(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Parameter(description = "ID of the user to retrieve", example = "12")
            @PathVariable Long userId
    ) {
        return userManagementService.getUser(principal.userId(), userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create user", description = "Creates a new user in the tenant")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    public UserManagementResponse createUser(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UserCreateRequest request
    ) {
        return userManagementService.createUser(principal.userId(), request);
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user", description = "Updates an existing user in the tenant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public UserManagementResponse updateUser(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Parameter(description = "ID of the user to update", example = "12")
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return userManagementService.updateUser(principal.userId(), userId, request);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete user", description = "Deletes a user from the tenant")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public void deleteUser(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Parameter(description = "ID of the user to delete", example = "12")
            @PathVariable Long userId
    ) {
        userManagementService.deleteUser(principal.userId(), userId);
    }
}

