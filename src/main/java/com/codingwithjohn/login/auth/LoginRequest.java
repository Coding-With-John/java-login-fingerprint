package com.codingwithjohn.login.auth;

/**
 * What the login page sends. {@code requestId} is the Fingerprint event id
 * from the JavaScript agent (the v4 agent calls it {@code event_id}); it
 * may be a {@code stub-...} placeholder when no public key is configured.
 */
public record LoginRequest(String username, String password, String requestId) {
}
