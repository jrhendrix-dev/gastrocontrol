package com.gastrocontrol.gastrocontrol.config.multitenancy;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * A {@link AbstractRoutingDataSource} that routes each connection request
 * to the correct MySQL schema based on the current {@link TenantContext}.
 *
 * <p>Spring's {@link AbstractRoutingDataSource} calls
 * {@link #determineCurrentLookupKey()} for every connection acquisition.
 * We return the schema name, which is used as the key to select from the
 * configured target data sources map.
 *
 * <p>Since we use a single underlying DataSource (same MySQL server, same
 * credentials), we don't actually have multiple DataSources in the map.
 * Instead, we use a {@link SchemaRoutingDataSource} wrapper that issues
 * a {@code USE <schema>} statement after each connection is obtained.
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    /**
     * Returns the current schema name as the lookup key.
     * Spring uses this to select the appropriate DataSource from the
     * configured target data sources.
     *
     * @return current schema name from {@link TenantContext}
     */
    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getSchema();
    }
}