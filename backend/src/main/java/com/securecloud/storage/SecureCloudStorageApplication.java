package com.securecloud.storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Add dotenv import
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class SecureCloudStorageApplication {

	public static void main(String[] args) {
		// Load .env file if present (for local development)
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

		SpringApplication.run(SecureCloudStorageApplication.class, args);
	}

}
