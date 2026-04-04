package com.gastrocontrol.gastrocontrol.demo;

import com.gastrocontrol.gastrocontrol.config.multitenancy.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.UUID;

/**
 * Manages the lifecycle of demo session schemas.
 *
 * <p>When a visitor arrives at the GastroControl demo, this service:
 * <ol>
 *   <li>Generates a unique session ID</li>
 *   <li>Creates a MySQL schema named {@code gc_demo_<id>}</li>
 *   <li>Runs all Flyway migrations against that schema</li>
 *   <li>Seeds the schema with demo user accounts</li>
 *   <li>Records the session in the {@code demo_sessions} tracking table</li>
 * </ol>
 *
 * <p>All schema management operations use a dedicated admin
 * {@link JdbcTemplate} that bypasses {@link com.gastrocontrol.gastrocontrol.config.multitenancy.SchemaAwareDataSource}
 * and always connects to the default schema. This avoids any dependency
 * on {@link TenantContext} during provisioning.
 */
@Slf4j
@Service
public class DemoSessionService {

    /** How long a demo session lives before being eligible for cleanup. */
    static final int SESSION_TTL_HOURS = 2;

    /**
     * Admin JdbcTemplate always connected to the default schema.
     * Used for schema creation, session tracking, and seed data insertion.
     */
    private final JdbcTemplate adminJdbc;

    /** The raw datasource properties from application.yaml, used to build Flyway. */
    private final DataSourceProperties dataSourceProperties;

    /**
     * Constructs the service using an admin DataSource that always targets
     * the default schema, independent of any tenant context.
     *
     * @param dataSourceProperties Spring Boot datasource config from application.yaml
     */
    public DemoSessionService(DataSourceProperties dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
        this.adminJdbc = new JdbcTemplate(buildAdminDataSource(dataSourceProperties));
    }

    /**
     * Provisions a new demo session schema and returns the session ID.
     *
     * @return the generated session ID to be stored by the client
     *         and sent as {@code X-Demo-Session} on subsequent requests
     */
    public String provisionSession() {
        String sessionId = generateSessionId();
        String schema    = "gc_demo_" + sessionId;

        log.info("Provisioning demo schema: {}", schema);

        createSchema(schema);
        runMigrations(schema);
        seedDemoUsers(schema);
        recordSession(sessionId, schema);

        log.info("Demo schema '{}' ready", schema);
        return sessionId;
    }

    /**
     * Creates the MySQL schema if it does not already exist.
     *
     * @param schema the schema name to create
     */
    private void createSchema(String schema) {
        adminJdbc.execute(
                "CREATE SCHEMA IF NOT EXISTS `" + schema + "` " +
                        "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    }

    /**
     * Runs all versioned Flyway migrations against the new schema.
     * Dev-only repeatable migrations are excluded.
     *
     * @param schema the target schema name
     */
    private void runMigrations(String schema) {
        // Build a schema-specific DataSource for Flyway
        DriverManagerDataSource schemaDs = new DriverManagerDataSource();
        schemaDs.setDriverClassName(dataSourceProperties.getDriverClassName());
        schemaDs.setUrl(buildUrlForSchema(schema));
        schemaDs.setUsername(dataSourceProperties.getUsername());
        schemaDs.setPassword(dataSourceProperties.getPassword());

        Flyway flyway = Flyway.configure()
                .dataSource(schemaDs)
                .schemas(schema)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .cleanDisabled(true)
                .load();

        flyway.migrate();
        log.debug("Flyway migrations complete for schema: {}", schema);
    }

    /**
     * Seeds the new schema with demo user accounts.
     * Password for all demo accounts: {@code GastroControl1726!}
     *
     * @param schema the target schema name
     */
    private void seedDemoUsers(String schema) {
        JdbcTemplate schemaJdbc = new JdbcTemplate(buildSchemaDataSource(schema));

        schemaJdbc.update("""
                INSERT INTO users (email, password, role, active, created_at, first_name, last_name, phone)
                VALUES
                    ('admin@gastro.demo',
                     '$2b$12$3jae2iKs5BcKJa/m5TkLx.4b6w2ELF1irYFYXbJutSbkCBW5uTTdy',
                     'ADMIN', 1, NOW(), 'Demo', 'Admin', '600000001'),
                    ('manager@gastro.demo',
                     '$2b$12$/oIg33gYYwzS02/.GRZF6.5NyYW.Xdqoef5M5laEjC11XdEOeEQ/G',
                     'MANAGER', 1, NOW(), 'Demo', 'Manager', '600000002'),
                    ('staff@gastro.demo',
                     '$2b$12$F1St7LtgqmHMsREXI6oEo.jDu6tcM5HjgcU3fvMNojZ1.o/j5vkTq',
                     'STAFF', 1, NOW(), 'Demo', 'Staff', '600000003')
                ON DUPLICATE KEY UPDATE email = email
                """);

        log.debug("Demo users seeded for schema: {}", schema);
    }

    /**
     * Records the session in the {@code demo_sessions} tracking table
     * in the default schema so the cleanup job can find it later.
     *
     * @param sessionId the session ID
     * @param schema    the schema name
     */
    private void recordSession(String sessionId, String schema) {
        adminJdbc.update("""
                INSERT INTO demo_sessions (session_id, schema_name, created_at, expires_at)
                VALUES (?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? HOUR))
                """,
                sessionId, schema, SESSION_TTL_HOURS);
    }

    /**
     * Generates a short, URL-safe session ID derived from a random UUID.
     *
     * @return a 12-character alphanumeric session ID
     */
    private String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /**
     * Builds a raw {@link DataSource} that always connects to the default
     * schema. Used for admin operations like schema creation and session
     * tracking that must never be subject to tenant routing.
     *
     * @param props Spring Boot datasource properties
     * @return a {@link DriverManagerDataSource} pointed at the default schema
     */
    private DataSource buildAdminDataSource(DataSourceProperties props) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(props.getDriverClassName());
        ds.setUrl(props.getUrl());
        ds.setUsername(props.getUsername());
        ds.setPassword(props.getPassword());
        return ds;
    }

    /**
     * Builds a {@link DataSource} pointed at a specific schema by name.
     * Used to seed demo data directly into the newly created schema.
     *
     * @param schema the schema name to connect to
     * @return a {@link DriverManagerDataSource} for the given schema
     */
    private DataSource buildSchemaDataSource(String schema) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(dataSourceProperties.getDriverClassName());
        ds.setUrl(buildUrlForSchema(schema));
        ds.setUsername(dataSourceProperties.getUsername());
        ds.setPassword(dataSourceProperties.getPassword());
        return ds;
    }

    /**
     * Builds a JDBC URL for a specific schema by replacing the database
     * name in the base URL from application.yaml.
     *
     * @param schema the target schema name
     * @return a JDBC URL pointing at the given schema
     */
    private String buildUrlForSchema(String schema) {
        // Replace the database name segment in the base URL
        // e.g. jdbc:mysql://host:3306/gastrocontrol?... → jdbc:mysql://host:3306/gc_demo_xxx?...
        String baseUrl = dataSourceProperties.getUrl();
        return baseUrl.replaceFirst("(jdbc:mysql://[^/]+/)[^?]+", "$1" + schema);
    }
}