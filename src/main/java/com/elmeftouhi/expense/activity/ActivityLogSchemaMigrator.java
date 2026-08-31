package com.elmeftouhi.expense.activity;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class ActivityLogSchemaMigrator {

    private static final String CONSTRAINT_NAME = "activity_logs_subject_type_check";

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void refreshSubjectTypeCheckConstraint() {
        String subjectTypeValues = Arrays.stream(ActivitySubjectType.values())
                .map(Enum::name)
                .map(value -> "'" + value + "'")
                .collect(Collectors.joining(", "));

        jdbcTemplate.execute("ALTER TABLE activity_logs DROP CONSTRAINT IF EXISTS " + CONSTRAINT_NAME);
        jdbcTemplate.execute("ALTER TABLE activity_logs ADD CONSTRAINT " + CONSTRAINT_NAME
                + " CHECK (subject_type IN (" + subjectTypeValues + "))");
    }
}

