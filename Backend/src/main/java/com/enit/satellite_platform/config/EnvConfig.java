package com.enit.satellite_platform.config;

/**
 * Configuration class for loading environment variables from a .env file.
 * This class is responsible for loading environment variables during application startup
 * and setting them as system properties to be accessible throughout the application.
 *
 * Uses the dotenv library to read environment variables from a .env file and
 * make them available to the application through System.getProperty().
 */
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
@Configuration
public class EnvConfig {

    @PostConstruct
    public void loadEnv() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .ignoreIfMalformed()
                    .load();
            
            if (dotenv != null) {
                dotenv.entries().forEach(entry -> {
                    System.setProperty(entry.getKey(), entry.getValue());
                });
                System.out.println("Loaded environment variables from .env file");
            }
        } catch (Exception e) {
            // .env file is optional, environment variables can be set directly
            System.out.println("No .env file found, using environment variables directly: " + e.getMessage());
        }
    }
}
