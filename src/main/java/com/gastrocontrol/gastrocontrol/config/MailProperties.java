package com.gastrocontrol.gastrocontrol.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mail")
public record MailProperties(
        boolean enabled,
        From from,
        Mailgun mailgun
) {
    public record From(String email, String name) {}
    public record Mailgun(String apiKey, String domain, String baseUrl) {}
}
