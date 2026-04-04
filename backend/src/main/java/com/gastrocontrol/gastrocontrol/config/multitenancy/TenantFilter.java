package com.gastrocontrol.gastrocontrol.config.multitenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that extracts the demo session token from the
 * {@code X-Demo-Session} request header and sets the appropriate schema
 * in {@link TenantContext} for the duration of the request.
 *
 * <p>If no header is present the request is routed to the default
 * {@value TenantContext#DEFAULT_SCHEMA} schema, which serves non-demo
 * traffic (e.g. health checks, Swagger).
 *
 * <p>The tenant context is always cleared in a {@code finally} block
 * to prevent schema leakage across requests on pooled threads.
 */
@Slf4j
@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {

    /** Header name used by the Angular frontend to pass the demo session ID. */
    public static final String TENANT_HEADER = "X-Demo-Session";

    /** Prefix for all demo schema names. */
    public static final String DEMO_SCHEMA_PREFIX = "gc_demo_";

    /**
     * Resolves the target schema from the request header and sets it in
     * {@link TenantContext} before passing the request down the filter chain.
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the remaining filter chain
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String sessionToken = request.getHeader(TENANT_HEADER);

        try {
            if (sessionToken != null && !sessionToken.isBlank()) {
                String schema = DEMO_SCHEMA_PREFIX + sanitize(sessionToken);
                TenantContext.setSchema(schema);
                log.trace("Tenant set to schema '{}' for request: {} {}",
                        schema, request.getMethod(), request.getRequestURI());
            } else {
                TenantContext.setSchema(TenantContext.DEFAULT_SCHEMA);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Strips any characters that are not alphanumeric or underscores from
     * the session token before using it as part of a schema name.
     *
     * @param token raw token from the HTTP header
     * @return sanitized token safe for use in a schema name
     */
    private String sanitize(String token) {
        return token.replaceAll("[^a-zA-Z0-9_]", "").substring(0, Math.min(token.length(), 32));
    }
}