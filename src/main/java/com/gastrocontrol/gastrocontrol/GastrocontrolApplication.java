package com.gastrocontrol.gastrocontrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class GastrocontrolApplication {

	public static void main(String[] args) {
		SpringApplication.run(GastrocontrolApplication.class, args);
	}

}
