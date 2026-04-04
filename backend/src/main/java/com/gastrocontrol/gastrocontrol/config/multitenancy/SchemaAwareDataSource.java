package com.gastrocontrol.gastrocontrol.config.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A {@link DataSource} decorator that transparently switches the active MySQL
 * schema on every connection by issuing {@code USE <schema>} immediately after
 * the connection is obtained from the underlying pool.
 *
 * <p>This avoids the complexity of Hibernate's full multi-tenancy SPI while
 * achieving complete schema-level isolation between demo sessions.
 *
 * <p>Schema names are validated against a strict allowlist pattern before
 * being used in the SQL statement to prevent SQL injection.
 */
@Slf4j
public class SchemaAwareDataSource extends DelegatingDataSource {

    /** Pattern that all valid schema names must match. */
    private static final java.util.regex.Pattern SAFE_SCHEMA =
            java.util.regex.Pattern.compile("^[a-zA-Z0-9_]{1,64}$");

    /**
     * Constructs a {@link SchemaAwareDataSource} wrapping the given delegate.
     *
     * @param delegate the underlying pooled DataSource
     */
    public SchemaAwareDataSource(DataSource delegate) {
        super(delegate);
    }

    /**
     * Obtains a connection from the underlying pool and switches it to the
     * schema determined by {@link TenantContext#getSchema()}.
     *
     * @return a connection already pointed at the correct schema
     * @throws SQLException if the connection cannot be obtained or the
     *                      {@code USE} statement fails
     */
    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = obtainTargetDataSource().getConnection();
        switchSchema(connection);
        return connection;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Switches the schema on connections obtained with explicit credentials.
     */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = obtainTargetDataSource().getConnection(username, password);
        switchSchema(connection);
        return connection;
    }

    /**
     * Issues a {@code USE <schema>} statement on the given connection.
     *
     * @param connection the connection to switch
     * @throws SQLException              if the statement fails
     * @throws IllegalArgumentException  if the schema name is invalid
     */
    private void switchSchema(Connection connection) throws SQLException {
        String schema = TenantContext.getSchema();
        if (!SAFE_SCHEMA.matcher(schema).matches()) {
            throw new IllegalArgumentException(
                    "Invalid schema name rejected for security: " + schema);
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("USE `" + schema + "`");
            log.trace("Switched connection to schema: {}", schema);
        }
    }
}