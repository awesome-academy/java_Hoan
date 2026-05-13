package com.fooddrinks.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * One-time schema migration: changes the {@code users.provider} column enum
 * from {@code TWITTER} to {@code APPLE}.
 *
 * Runs at startup (order=1, before AdminDataInitializer) via ApplicationRunner.
 * Idempotent — skips if the column already contains APPLE instead of TWITTER.
 *
 * Migration sequence:
 *   1. UPDATE any TWITTER rows to APPLE (should not exist in normal development,
 *      but handled defensively).
 *   2. ALTER TABLE to replace TWITTER with APPLE in the ENUM definition.
 *
 * Note on timing: Hibernate's ddl-auto:update runs before ApplicationRunners.
 * If the DB contains TWITTER rows and MySQL strict mode prevents the ALTER,
 * Hibernate may log an error but continue starting. This runner then performs
 * the explicit migration and resolves the inconsistency.
 */
@Slf4j
@Component
@Order(1) // run before AdminDataInitializer
@RequiredArgsConstructor
public class ProviderMigrationRunner implements ApplicationRunner {

    private static final String CHECK_COLUMN_SQL = """
            SELECT COLUMN_TYPE
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME   = 'users'
              AND COLUMN_NAME  = 'provider'
            """;

    private static final String MIGRATE_DATA_SQL =
            "UPDATE users SET provider = 'APPLE' WHERE provider = 'TWITTER'";

    private static final String ALTER_COLUMN_SQL = """
            ALTER TABLE users
              MODIFY COLUMN provider
              ENUM('LOCAL','GOOGLE','FACEBOOK','APPLE') NOT NULL DEFAULT 'LOCAL'
            """;

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            String columnType = queryColumnType(conn);
            if (columnType == null) {
                log.debug("ProviderMigration: 'users' table not yet created — skipping");
                return;
            }
            if (!columnType.toUpperCase().contains("TWITTER")) {
                log.debug("ProviderMigration: already up-to-date, skipping");
                return;
            }

            log.info("ProviderMigration: starting — renaming TWITTER → APPLE in users.provider");

            // Step 1: migrate any existing TWITTER rows
            try (PreparedStatement ps = conn.prepareStatement(MIGRATE_DATA_SQL)) {
                int updated = ps.executeUpdate();
                if (updated > 0) {
                    log.warn("ProviderMigration: {} row(s) with provider=TWITTER were migrated to APPLE",
                             updated);
                }
            }

            // Step 2: alter the ENUM column to replace TWITTER with APPLE
            try (PreparedStatement ps = conn.prepareStatement(ALTER_COLUMN_SQL)) {
                ps.executeUpdate();
            }

            log.info("ProviderMigration: completed successfully");

        } catch (Exception e) {
            log.error("ProviderMigration failed. Run the following SQL manually:\n"
                      + "  UPDATE users SET provider = 'APPLE' WHERE provider = 'TWITTER';\n"
                      + "  ALTER TABLE users MODIFY COLUMN provider "
                      + "ENUM('LOCAL','GOOGLE','FACEBOOK','APPLE') NOT NULL DEFAULT 'LOCAL';",
                      e);
        }
    }

    private String queryColumnType(Connection conn) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(CHECK_COLUMN_SQL);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString("COLUMN_TYPE") : null;
        }
    }
}
