package com.gastrocontrol.gastrocontrol.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled job that drops expired demo schemas and removes their
 * tracking records from the {@code demo_sessions} table.
 *
 * <p>Uses a dedicated admin {@link JdbcTemplate} that always connects
 * to the default schema, bypassing tenant routing entirely.
 *
 * <p>Runs at the interval configured by {@code demo.cleanup.interval-ms}
 * (default: every 30 minutes).
 */
@Slf4j
@Component
public class DemoCleanupJob {

    private final JdbcTemplate adminJdbc;

    /**
     * Constructs the cleanup job with a direct connection to the default schema.
     *
     * @param props Spring Boot datasource properties
     */
    public DemoCleanupJob(DataSourceProperties props) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(props.getDriverClassName());
        ds.setUrl(props.getUrl());
        ds.setUsername(props.getUsername());
        ds.setPassword(props.getPassword());
        this.adminJdbc = new JdbcTemplate(ds);
    }

    /**
     * Finds all expired demo sessions and drops their schemas.
     *
     * @see DemoSessionService#SESSION_TTL_HOURS
     */
    @Scheduled(fixedDelayString = "${demo.cleanup.interval-ms:1800000}")
    public void cleanupExpiredSessions() {
        log.info("Running demo session cleanup job");

        List<String> expiredSchemas = adminJdbc.queryForList(
                "SELECT schema_name FROM demo_sessions WHERE expires_at < NOW()",
                String.class);

        if (expiredSchemas.isEmpty()) {
            log.info("No expired demo sessions found");
            return;
        }

        log.info("Dropping {} expired demo schema(s): {}", expiredSchemas.size(), expiredSchemas);

        for (String schema : expiredSchemas) {
            try {
                adminJdbc.execute("DROP SCHEMA IF EXISTS `" + schema + "`");
                adminJdbc.update(
                        "DELETE FROM demo_sessions WHERE schema_name = ?", schema);
                log.info("Dropped demo schema: {}", schema);
            } catch (Exception e) {
                log.error("Failed to drop demo schema '{}': {}", schema, e.getMessage());
            }
        }
    }
}