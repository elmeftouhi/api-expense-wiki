package com.elmeftouhi.expense.expense.controller;

import com.elmeftouhi.expense.expense.service.SubscriptionService;
import com.elmeftouhi.expense.expense.SubscriptionLifecycleStatus;
import com.elmeftouhi.expense.expense.SubscriptionFrequency;
import com.elmeftouhi.expense.expense.dto.SubscriptionCreateRequest;
import com.elmeftouhi.expense.expense.dto.SubscriptionPageResponse;
import com.elmeftouhi.expense.expense.dto.SubscriptionResponse;
import com.elmeftouhi.expense.expense.dto.SubscriptionResumeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/subscriptions")
@Tag(name = "Subscriptions", description = "Manage independent recurring subscription definitions and lifecycle")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create subscription", description = "Creates an independent recurring subscription definition")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Subscription created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    public SubscriptionResponse create(@Valid @RequestBody SubscriptionCreateRequest request) {
        return subscriptionService.create(request);
    }

    @GetMapping
    @Operation(summary = "List subscriptions", description = "Returns paginated subscriptions with filtering and sorting")
    @ApiResponse(responseCode = "200", description = "Subscriptions returned successfully")
    public SubscriptionPageResponse findAll(
            @Parameter(description = "Search by name or description")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by lifecycle status")
            @RequestParam(required = false) SubscriptionLifecycleStatus status,
            @Parameter(description = "Filter by category IDs")
            @RequestParam(required = false) List<Long> categoryIds,
            @Parameter(description = "Filter by recurrence frequency")
            @RequestParam(required = false) SubscriptionFrequency frequency,
            @Parameter(description = "Filter by next billing date from")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nextBillingFrom,
            @Parameter(description = "Filter by next billing date to")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nextBillingTo,
            @PageableDefault(size = 20, sort = "nextBillingDate", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<SubscriptionResponse> pageResult = subscriptionService.search(
                search,
                status,
                categoryIds,
                frequency,
                nextBillingFrom,
                nextBillingTo,
                pageable
        );
        return new SubscriptionPageResponse(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get subscription by ID", description = "Returns details of a single subscription")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subscription returned successfully"),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    public SubscriptionResponse getById(@PathVariable Long id) {
        return subscriptionService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update subscription", description = "Updates subscription details and future schedule")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subscription updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    public SubscriptionResponse update(@PathVariable Long id, @Valid @RequestBody SubscriptionCreateRequest request) {
        return subscriptionService.update(id, request);
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause subscription", description = "Pauses future generation and reminders")
    @ApiResponse(responseCode = "200", description = "Subscription paused successfully")
    public SubscriptionResponse pause(@PathVariable Long id) {
        return subscriptionService.pause(id);
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume subscription", description = "Resumes subscription with an explicit next billing date")
    @ApiResponse(responseCode = "200", description = "Subscription resumed successfully")
    public SubscriptionResponse resume(@PathVariable Long id, @Valid @RequestBody SubscriptionResumeRequest request) {
        return subscriptionService.resume(id, request.nextBillingDate());
    }

    @PostMapping("/{id}/stop")
    @Operation(summary = "Stop subscription", description = "Permanently stops future generation and reminders")
    @ApiResponse(responseCode = "200", description = "Subscription stopped successfully")
    public SubscriptionResponse stop(@PathVariable Long id) {
        return subscriptionService.stop(id);
    }

}

