package com.elmeftouhi.expense.activity;

import com.elmeftouhi.expense.activity.dto.ActivityLogPageResponse;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/activity-logs")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping
    public ActivityLogPageResponse findAll(
            @RequestParam(required = false) ActivityAction action,
            @RequestParam(required = false) ActivitySubjectType subjectType,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String actor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return activityLogService.search(action, subjectType, subjectId, from, to, actor, page, size);
    }
}

