package com.experimentalassistant.backend;

import com.experimentalassistant.backend.config.DotEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		DotEnvLoader.loadIfPresent();
		SpringApplication.run(BackendApplication.class, args);
	}

}
