package com.codingwithjohn.login;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Simple Spring Boot login system with a Fingerprint device-intelligence
 * check on login. Starter code for a Coding With John video.
 *
 * Run with: ./mvnw spring-boot:run
 * Then open: http://localhost:8080
 *
 * Demo users (see DataInitializer): demo / password123, alice / correct-horse
 */
@SpringBootApplication
public class LoginApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoginApplication.class, args);
    }
}
