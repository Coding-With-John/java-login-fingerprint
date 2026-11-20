package com.codingwithjohn.login.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The demo user list, hardcoded for the video. This is not production
 * user storage; a real app would keep users in a database. Demo
 * passwords only, never use these in a real app.
 */
@Component
public class UserStore {

    private final Map<String, User> users = new HashMap<>();

    public UserStore(PasswordEncoder passwords) {
        addUser(passwords, "demo", "password123", "Demo User");
        addUser(passwords, "alice", "correct-horse", "Alice");
    }

    private void addUser(PasswordEncoder passwords, String username, String plainPassword, String displayName) {
        users.put(username, new User(username, passwords.encode(plainPassword), displayName));
    }

    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }
}
