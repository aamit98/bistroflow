package com.gitProjects.adss_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AdssBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdssBackendApplication.class, args);
	}

}
