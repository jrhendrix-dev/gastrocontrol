package com.gastrocontrol.gastrocontrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the GastroControl Spring Boot application.
 *
 * <p>{@link EnableScheduling} activates Spring's scheduled task infrastructure,
 * required by {@link com.gastrocontrol.gastrocontrol.demo.DemoCleanupJob}
 * to periodically drop expired demo session schemas.
 */
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class GastrocontrolApplication {

	public static void main(String[] args) {
		SpringApplication.run(GastrocontrolApplication.class, args);
	}
}