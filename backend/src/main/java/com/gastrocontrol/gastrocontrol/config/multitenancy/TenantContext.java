package com.gastrocontrol.gastrocontrol.config.multitenancy;

/**
 * Thread-local holder for the current tenant's schema name.
 *
 * We use an {@link InheritableThreadLocal} so that child threads
 * (e.g. async tasks) inherit the tenant context automatically.
 * Always call {@link #clear()} in a finally block to avoid leaking
 * tenant context across requests.
 */
public final class TenantContext {

    /** Schema name used when no demo session is active (the base schema). */
    public static final String DEFAULT_SCHEMA = "gastrocontrol";

    private static final InheritableThreadLocal<String> CURRENT_SCHEMA =
            new InheritableThreadLocal<>();

    private TenantContext() {}

    /**
     * Sets the current schema name for this thread.
     *
     * @param schema the MySQL schema name to route to
     */
    public static void setSchema(String schema) {
        CURRENT_SCHEMA.set(schema);
    }

    /**
     * Returns the current schema name, defaulting to {@value #DEFAULT_SCHEMA}
     * if none has been set.
     *
     * @return current schema name; never {@code null}
     */
    public static String getSchema() {
        String schema = CURRENT_SCHEMA.get();
        return (schema != null) ? schema : DEFAULT_SCHEMA;
    }

    /**
     * Clears the tenant context for this thread.
     * Must be called after every request to prevent thread pool leaks.
     */
    public static void clear() {
        CURRENT_SCHEMA.remove();
    }
}