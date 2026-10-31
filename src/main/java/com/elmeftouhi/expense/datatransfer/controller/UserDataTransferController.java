package com.elmeftouhi.expense.datatransfer.controller;

import com.elmeftouhi.expense.datatransfer.dto.ImportUserDataRequest;
import com.elmeftouhi.expense.datatransfer.dto.ImportUserDataResponse;
import com.elmeftouhi.expense.datatransfer.dto.UserDataBackupSnapshot;
import com.elmeftouhi.expense.datatransfer.service.UserDataTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/data-transfer")
@RequiredArgsConstructor
@Tag(name = "Data Transfer", description = "Export and import workspace data backups")
public class UserDataTransferController {

    private final UserDataTransferService userDataTransferService;

    @GetMapping("/export")
    @Operation(summary = "Export workspace data", description = "Exports the selected tenant data as a portable JSON payload")
    @ApiResponse(responseCode = "200", description = "Workspace data exported successfully")
    public UserDataBackupSnapshot exportCurrentTenantData() {
        return userDataTransferService.exportCurrentTenantData();
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Import workspace data", description = "Imports a backup into a newly created tenant owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Workspace data imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid backup payload"),
            @ApiResponse(responseCode = "409", description = "Tenant name already exists")
    })
    public ImportUserDataResponse importIntoNewTenant(@Valid @RequestBody ImportUserDataRequest request) {
        return userDataTransferService.importIntoNewTenant(request);
    }
}

