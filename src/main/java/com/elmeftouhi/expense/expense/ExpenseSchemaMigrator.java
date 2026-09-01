package com.elmeftouhi.expense.expense;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class ExpenseSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initializeExpenseDefaults() {
        // Set defaults for any expenses without currency
        String sql = "UPDATE expenses SET currency = 'USD' WHERE currency IS NULL";
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            // Table might not exist yet, skip
        }

        // Set defaults for status
        String statusSql = "UPDATE expenses SET status = 'PENDING' WHERE status IS NULL";
        try {
            jdbcTemplate.execute(statusSql);
        } catch (Exception e) {
            // Table might not exist yet, skip
        }
    }
}

