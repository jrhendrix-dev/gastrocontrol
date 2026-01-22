package com.gastrocontrol.gastrocontrol.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();

        // Explicit origin required when allowCredentials=true
        c.setAllowedOrigins(List.of("http://localhost:4200"));
        c.setAllowCredentials(true);

        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // include any headers you send (Authorization, Content-Type, etc.)
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));

        // optional: Set-Cookie is the important one for your refresh cookie
        c.setExposedHeaders(List.of("Set-Cookie"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }
}
