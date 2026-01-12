package com.gastrocontrol.gastrocontrol.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application-level URLs used to build links inside emails and other external-facing messages.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        PublicUrls publicUrls,
        FrontendUrls frontend
) {
    public record PublicUrls(String baseUrl) {}
    public record FrontendUrls(String baseUrl) {}
}
