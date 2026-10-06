package com.odontogestion.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE clinical_records MODIFY COLUMN appointment_id BIGINT NULL");
            log.info("Successfully altered clinical_records.appointment_id column to allow NULL values");
        } catch (Exception e) {
            log.warn("Database initialization step skipped or column already modified: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE appointments ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'");
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute("ALTER TABLE appointments ADD COLUMN payment_method VARCHAR(30) NULL");
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute("ALTER TABLE appointments ADD COLUMN payment_date DATETIME NULL");
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute("ALTER TABLE appointments ADD COLUMN amount DECIMAL(10,2) NOT NULL DEFAULT 0.00");
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute("ALTER TABLE appointments ADD COLUMN payment_notes TEXT NULL");
        } catch (Exception ignored) {}
    }
}
