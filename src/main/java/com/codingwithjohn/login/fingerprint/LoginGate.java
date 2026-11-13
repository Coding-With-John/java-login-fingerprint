package com.codingwithjohn.login.fingerprint;

import org.springframework.stereotype.Component;

/**
 * The Fingerprint gate that runs BEFORE the password check on every login.
 *
 * Decision order, cheapest and most certain first:
 * <ol>
 *   <li>Bot detected: reject outright (403). No point checking a password
 *       for an automated login attempt.</li>
 *   <li>Stuffing velocity: this device has already failed too many logins
 *       in the last 10 minutes, so reject with "try again later" (429).</li>
 *   <li>Unrecognized device: the user has logged in before, but never from
 *       this device. Allow the login but flag step-up, so the app can ask
 *       for extra verification. (This starter only flags it, see TODO.)</li>
 * </ol>
 *
 * Fingerprint only supplies the signals. Every allow / step-up / block
 * decision here is our own code.
 */
@Component
public class LoginGate {

    /** Failed logins from one device inside the tracker window before we block. */
    public static final int MAX_FAILED_ATTEMPTS = 5;

    private final FingerprintService fingerprintService;
    private final LoginAttemptTracker tracker;

    public LoginGate(FingerprintService fingerprintService, LoginAttemptTracker tracker) {
        this.fingerprintService = fingerprintService;
        this.tracker = tracker;
    }

    /**
     * @param allowed whether the login may proceed to the password check
     * @param stepUp  true when the login may proceed but needs extra verification
     * @param status  HTTP status to return when not allowed (403 or 429)
     * @param reason  human-readable reason, shown to the user
     * @param check   the underlying Fingerprint signals for this attempt
     */
    public record GateDecision(boolean allowed, boolean stepUp, int status, String reason, VisitorCheck check) {
    }

    public GateDecision check(String requestId, String username) {
        VisitorCheck check = fingerprintService.check(requestId);

        if (check.botDetected()) {
            return new GateDecision(false, false, 403, "Automated login attempt detected", check);
        }
        if (tracker.failedAttemptsInWindow(check.visitorId()) >= MAX_FAILED_ATTEMPTS) {
            return new GateDecision(false, false, 429,
                    "Too many failed logins from this device, try again later", check);
        }
        if (tracker.hasKnownDevices(username) && !tracker.isKnownDevice(username, check.visitorId())) {
            // TODO: step-up is only flagged here, not enforced. A real version
            // would send a one-time code or ask a security question before
            // completing the login, instead of just returning stepUp=true.
            return new GateDecision(true, true, 200,
                    "Unrecognized device, extra verification required", check);
        }
        return new GateDecision(true, false, 200, "OK", check);
    }
}
