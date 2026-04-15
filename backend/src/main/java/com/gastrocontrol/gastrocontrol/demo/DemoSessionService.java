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

    public String provisionSession() {
        String sessionId = generateSessionId();
        String schema    = "gc_demo_" + sessionId;

        log.info("Provisioning demo schema: {}", schema);

        createSchema(schema);
        runMigrations(schema);
        seedDemoUsers(schema);
        seedDemoData(schema);   // ← add this
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
     * Seeds the new schema with demo user accounts, using the same primary key
     * IDs as the corresponding users in the main schema.
     *
     * <p>This is critical for correctness: the JWT issued after demo login contains
     * the user's ID from the main schema. When {@code /api/me} or any other endpoint
     * queries the demo schema by that ID, the row must exist with the same ID.</p>
     *
     * @param schema the target schema name
     */
    private void seedDemoUsers(String schema) {
        // Fetch the IDs of the demo users from the main schema so we can
        // insert them with the same IDs into the demo schema.
        Long adminId   = adminJdbc.queryForObject(
                "SELECT id FROM users WHERE email = 'admin@gastro.demo'",   Long.class);
        Long managerId = adminJdbc.queryForObject(
                "SELECT id FROM users WHERE email = 'manager@gastro.demo'", Long.class);
        Long staffId   = adminJdbc.queryForObject(
                "SELECT id FROM users WHERE email = 'staff@gastro.demo'",   Long.class);

        JdbcTemplate schemaJdbc = new JdbcTemplate(buildSchemaDataSource(schema));

        schemaJdbc.update("""
            INSERT INTO users (id, email, password, role, active, created_at, first_name, last_name, phone)
            VALUES
                (?, 'admin@gastro.demo',
                 '$2b$12$3jae2iKs5BcKJa/m5TkLx.4b6w2ELF1irYFYXbJutSbkCBW5uTTdy',
                 'ADMIN', 1, NOW(), 'Demo', 'Admin', '600000001'),
                (?, 'manager@gastro.demo',
                 '$2b$12$/oIg33gYYwzS02/.GRZF6.5NyYW.Xdqoef5M5laEjC11XdEOeEQ/G',
                 'MANAGER', 1, NOW(), 'Demo', 'Manager', '600000002'),
                (?, 'staff@gastro.demo',
                 '$2b$12$F1St7LtgqmHMsREXI6oEo.jDu6tcM5HjgcU3fvMNojZ1.o/j5vkTq',
                 'STAFF', 1, NOW(), 'Demo', 'Staff', '600000003')
            ON DUPLICATE KEY UPDATE email = email
            """,
                adminId, managerId, staffId);

        log.debug("Demo users seeded with main-schema IDs ({}, {}, {}) for schema: {}",
                adminId, managerId, staffId, schema);
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

    /**
     * Seeds the demo schema with categories, products (with image URLs),
     * and dining tables so the demo experience is fully populated.
     *
     * <p>All cleanup statements run on the same JDBC connection so that
     * {@code SET FOREIGN_KEY_CHECKS = 0} stays in effect for the deletes.</p>
     *
     * <p>Products reference the shared seed images on the uploads volume,
     * so no image files need to be copied per-schema.</p>
     *
     * @param schema the target demo schema name
     */
    private void seedDemoData(String schema) {
        JdbcTemplate schemaJdbc = new JdbcTemplate(buildSchemaDataSource(schema));

        // Clear any data inserted by Flyway dev migrations on the same connection
        // so the FK_CHECKS session variable stays in effect for all deletes.
        schemaJdbc.execute((java.sql.Connection conn) -> {
            try (var stmt = conn.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
                stmt.execute("DELETE FROM order_items");
                stmt.execute("DELETE FROM orders");
                stmt.execute("DELETE FROM products");
                stmt.execute("DELETE FROM categories");
                stmt.execute("DELETE FROM dining_tables");
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            return null;
        });

        // Categories
        schemaJdbc.update("""
            INSERT INTO categories (name) VALUES
                ('Hamburguesas'),
                ('Entrantes'),
                ('Ensaladas'),
                ('Pizzas'),
                ('Postres'),
                ('Bebidas')
            """);

        // Products with image URLs
        schemaJdbc.update("""
            INSERT INTO products (name, description, price_cents, active, category_id, image_url) VALUES
                ('Hamburguesa Clasica','Hamburguesa de ternera con lechuga, tomate y salsa de la casa.',1090,1,
                 (SELECT id FROM categories WHERE name='Hamburguesas'),
                 '/gastrocontrol/uploads/products/seed-hamburguesa-clasica.jpg'),
                ('Hamburguesa con Queso','Hamburguesa de ternera con queso cheddar.',1190,1,
                 (SELECT id FROM categories WHERE name='Hamburguesas'),
                 '/gastrocontrol/uploads/products/seed-hamburguesa-queso.jpg'),
                ('Doble Hamburguesa','Doble medallon de ternera con todos los ingredientes.',1490,1,
                 (SELECT id FROM categories WHERE name='Hamburguesas'),
                 '/gastrocontrol/uploads/products/seed-doble-hamburguesa.jpg'),
                ('Hamburguesa de Pollo','Pollo crujiente con mayonesa y lechuga.',1290,1,
                 (SELECT id FROM categories WHERE name='Hamburguesas'),
                 '/gastrocontrol/uploads/products/seed-hamburguesa-pollo.jpg'),
                ('Patatas Fritas','Patatas crujientes con sal.',350,1,
                 (SELECT id FROM categories WHERE name='Entrantes'),
                 '/gastrocontrol/uploads/products/seed-patatas-fritas.jpg'),
                ('Aros de Cebolla','Aros de cebolla dorados con salsa.',450,1,
                 (SELECT id FROM categories WHERE name='Entrantes'),
                 '/gastrocontrol/uploads/products/seed-aros-cebolla.jpg'),
                ('Nuggets de Pollo','6 nuggets de pollo con salsa a elegir.',590,1,
                 (SELECT id FROM categories WHERE name='Entrantes'),
                 '/gastrocontrol/uploads/products/seed-nuggets-pollo.jpg'),
                ('Ensalada Cesar','Lechuga romana, parmesano, picatostes y alino Cesar.',890,1,
                 (SELECT id FROM categories WHERE name='Ensaladas'),
                 '/gastrocontrol/uploads/products/seed-ensalada-cesar.jpg'),
                ('Ensalada Griega','Tomate, pepino, feta y aceitunas.',850,1,
                 (SELECT id FROM categories WHERE name='Ensaladas'),
                 '/gastrocontrol/uploads/products/seed-ensalada-griega.jpg'),
                ('Pizza Margherita','Tomate, mozzarella y albahaca fresca.',1090,1,
                 (SELECT id FROM categories WHERE name='Pizzas'),
                 '/gastrocontrol/uploads/products/seed-pizza-margherita.jpg'),
                ('Pizza Pepperoni','Pepperoni y mozzarella.',1290,1,
                 (SELECT id FROM categories WHERE name='Pizzas'),
                 '/gastrocontrol/uploads/products/seed-pizza-pepperoni.jpg'),
                ('Brownie de Chocolate','Brownie templado con helado de vainilla.',490,1,
                 (SELECT id FROM categories WHERE name='Postres'),
                 '/gastrocontrol/uploads/products/seed-brownie-chocolate.jpg'),
                ('Tarta de Queso','Porcion de tarta de queso cremosa.',520,1,
                 (SELECT id FROM categories WHERE name='Postres'),
                 '/gastrocontrol/uploads/products/seed-tarta-queso.jpg'),
                ('Coca-Cola','Lata 330ml.',250,1,
                 (SELECT id FROM categories WHERE name='Bebidas'),
                 '/gastrocontrol/uploads/products/seed-coca-cola.jpg'),
                ('Agua Mineral','Botella 500ml.',180,1,
                 (SELECT id FROM categories WHERE name='Bebidas'),
                 '/gastrocontrol/uploads/products/seed-agua-mineral.jpg')
            """);

        // Dining tables
        schemaJdbc.update("""
            INSERT INTO dining_tables (label) VALUES
                ('T1'),('T2'),('T3'),('T4'),('T5')
            """);

        log.debug("Demo data seeded for schema: {}", schema);
    }
}