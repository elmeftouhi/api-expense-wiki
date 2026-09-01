package com.elmeftouhi.expense.notification;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class NotificationSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initializeNotificationDefaults() {
        // Set defaults for notification status
        String sql = "UPDATE notifications SET status = 'SENT' WHERE status IS NULL";
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            // Table might not exist yet, skip
        }
    }
}

