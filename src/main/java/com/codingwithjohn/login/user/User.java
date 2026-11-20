package com.codingwithjohn.login.user;

/**
 * A user of the demo app. Passwords are stored as BCrypt hashes, never
 * in plain text. Deliberately small: this is a login demo, not a user
 * management system.
 */
public class User {

    private final String username;
    private final String passwordHash;
    private final String displayName;

    public User(String username, String passwordHash, String displayName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }
}
