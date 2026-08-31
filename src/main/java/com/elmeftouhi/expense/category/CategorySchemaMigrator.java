package com.elmeftouhi.expense.category;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class CategorySchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initializeCategoryLevels() {
        jdbcTemplate.execute("UPDATE categories SET level = 0 WHERE level IS NULL");
    }
}

