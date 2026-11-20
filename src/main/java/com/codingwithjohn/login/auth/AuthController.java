package com.codingwithjohn.login.auth;

import com.codingwithjohn.login.fingerprint.LoginAttemptTracker;
import com.codingwithjohn.login.fingerprint.LoginGate;
import com.codingwithjohn.login.user.User;
import com.codingwithjohn.login.user.UserStore;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * Login API. The important ordering is in {@link #login}: the Fingerprint
 * gate runs BEFORE the password check, so bots and stuffing attempts never
 * even get to try a password.
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserStore users;
    private final PasswordEncoder passwords;
    private final LoginGate gate;
    private final LoginAttemptTracker tracker;
    private final String publicKey;

    public AuthController(UserStore users, PasswordEncoder passwords,
                          LoginGate gate, LoginAttemptTracker tracker,
                          @Value("${fingerprint.public-key:}") String publicKey) {
        this.users = users;
        this.passwords = passwords;
        this.gate = gate;
        this.tracker = tracker;
        this.publicKey = publicKey;
    }

    /**
     * Tells the login page whether Fingerprint is configured, and hands it
     * the public key. Public keys are meant to be visible in the browser;
     * the secret key is never exposed here.
     */
    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of(
                "fingerprintConfigured", publicKey != null && !publicKey.isBlank(),
                "publicKey", publicKey == null ? "" : publicKey
        );
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request,
                                                     HttpSession session) {
        // 1. Fingerprint gate first, before any password work.
        LoginGate.GateDecision decision = gate.check(request.requestId(), request.username());
        if (!decision.allowed()) {
            return ResponseEntity.status(decision.status())
                    .body(Map.of("error", decision.reason(),
                            "fingerprint", decision.check().detail()));
        }

        // 2. Then the normal password check.
        Optional<User> user = users.findByUsername(request.username());
        if (user.isEmpty() || !passwords.matches(request.password(), user.get().getPasswordHash())) {
            tracker.recordFailure(decision.check().visitorId());
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid username or password"));
        }

        // 3. Success: remember this device and start a session.
        tracker.rememberDevice(request.username(), decision.check().visitorId());
        session.setAttribute("username", request.username());
        return ResponseEntity.ok(Map.of(
                "username", request.username(),
                "displayName", user.get().getDisplayName(),
                "stepUp", decision.stepUp(),
                "stepUpReason", decision.stepUp() ? decision.reason() : "",
                "visitorId", decision.check().visitorId(),
                "fingerprint", decision.check().detail()
        ));
    }

    /** Who is logged in right now, used by the protected page. */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }
        return ResponseEntity.ok(Map.of("username", username));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
