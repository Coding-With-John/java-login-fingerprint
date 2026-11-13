package com.codingwithjohn.login.config;

import com.codingwithjohn.login.user.AppUser;
import com.codingwithjohn.login.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the demo users on startup. Demo passwords only, never use these
 * in a real app.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder passwords;

    public DataInitializer(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Override
    public void run(String... args) {
        createIfMissing("demo", "password123", "Demo User");
        createIfMissing("alice", "correct-horse", "Alice");
    }

    private void createIfMissing(String username, String plainPassword, String displayName) {
        if (users.findByUsername(username).isEmpty()) {
            users.save(new AppUser(username, passwords.encode(plainPassword), displayName));
        }
    }
}
