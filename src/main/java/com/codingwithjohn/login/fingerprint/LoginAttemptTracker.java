package com.codingwithjohn.login.fingerprint;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory record of failed logins per device and known devices per user.
 *
 * This is what turns one Fingerprint signal (the visitor id) into a
 * credential-stuffing defence: many failed logins from the same visitor id
 * in a short window means one device hammering the login form.
 *
 * Demo only: everything resets when the app restarts. A real deployment
 * would keep this in Redis or the database.
 */
@Component
public class LoginAttemptTracker {

    /** Window for counting failed logins from one device. */
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final ConcurrentHashMap<String, Deque<Instant>> failuresByVisitor = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> devicesByUser = new ConcurrentHashMap<>();

    /** Record a failed login from this device. */
    public void recordFailure(String visitorId) {
        failuresByVisitor
                .computeIfAbsent(visitorId, key -> new ArrayDeque<>())
                .add(Instant.now());
    }

    /** Failed logins from this device inside the window. */
    public int failedAttemptsInWindow(String visitorId) {
        Deque<Instant> attempts = failuresByVisitor.get(visitorId);
        if (attempts == null) {
            return 0;
        }
        Instant cutoff = Instant.now().minus(WINDOW);
        synchronized (attempts) {
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
                attempts.removeFirst();
            }
            return attempts.size();
        }
    }

    /** Has this user ever logged in successfully from this device? */
    public boolean isKnownDevice(String username, String visitorId) {
        Set<String> devices = devicesByUser.get(username);
        return devices != null && devices.contains(visitorId);
    }

    /** Has this user ever logged in successfully from any device? */
    public boolean hasKnownDevices(String username) {
        Set<String> devices = devicesByUser.get(username);
        return devices != null && !devices.isEmpty();
    }

    /** Remember a device after a successful login. */
    public void rememberDevice(String username, String visitorId) {
        devicesByUser
                .computeIfAbsent(username, key -> ConcurrentHashMap.newKeySet())
                .add(visitorId);
    }
}
