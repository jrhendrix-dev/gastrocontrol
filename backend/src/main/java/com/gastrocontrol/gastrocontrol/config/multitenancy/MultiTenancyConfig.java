package com.gastrocontrol.gastrocontrol.config.multitenancy;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Configures multi-tenant data source routing.
 *
 * <p>We replace Spring Boot's default auto-configured {@link DataSource} with
 * a {@link SchemaAwareDataSource} that issues {@code USE <schema>} on every
 * connection based on the current {@link TenantContext}. This gives us full
 * schema-level isolation between demo sessions without needing to configure
 * multiple connection pools or disable JPA auto-configuration.
 *
 * <p>The underlying HikariCP pool is shared across all tenants — connections
 * are drawn from the same pool and then pointed at the correct schema.
 *
 * <p>We set {@code connectionInitSql} to {@code USE `gastrocontrol`} so that
 * HikariCP's internal connection validation succeeds at startup before any
 * tenant context is established.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
public class MultiTenancyConfig {

    /**
     * Creates the shared HikariCP connection pool and wraps it in a
     * {@link SchemaAwareDataSource} for per-request schema routing.
     *
     * @param properties Spring Boot's datasource properties from application.yaml
     * @return the primary {@link DataSource} used by JPA and Flyway
     */
    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(properties.getUrl());
        hikari.setUsername(properties.getUsername());
        hikari.setPassword(properties.getPassword());
        hikari.setDriverClassName(properties.getDriverClassName());
        hikari.setMaximumPoolSize(20);
        hikari.setMinimumIdle(2);
        hikari.setConnectionTimeout(30_000);
        hikari.setIdleTimeout(600_000);
        hikari.setMaxLifetime(1_800_000);
        hikari.setPoolName("GastroControlPool");

        // Ensures HikariCP connection validation succeeds at startup
        // by defaulting all connections to the base schema.
        // SchemaAwareDataSource will then override this per-request.
        hikari.setConnectionInitSql("USE `" + TenantContext.DEFAULT_SCHEMA + "`");

        HikariDataSource pool = new HikariDataSource(hikari);
        log.info("Initialized shared HikariCP pool for multi-tenant routing");
        return new SchemaAwareDataSource(pool);
    }
}