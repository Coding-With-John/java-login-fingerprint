package com.codingwithjohn.login.fingerprint;

/**
 * Checks a login attempt against Fingerprint device intelligence.
 *
 * The frontend identifies the visitor with the Fingerprint JavaScript agent
 * and sends the resulting event id with the login POST. Our own API calls
 * that id {@code requestId} (the v4 JavaScript agent calls it
 * {@code event_id}, the v3 agent called it {@code requestId}).
 * This service turns that id into the signals the login gate needs:
 * which device this is, and whether it looks automated or risky.
 *
 * Two implementations exist: a real one backed by the Fingerprint Java
 * Server SDK v8, and a stub that simulates signals so the app runs with no
 * API keys. See {@link FingerprintConfig} for how the active one is picked.
 */
public interface FingerprintService {

    /**
     * Look up the identification event behind {@code requestId}.
     *
     * @param requestId the event id from the Fingerprint JS agent; may be a
     *                  placeholder like {@code stub-...} when no public key
     *                  is configured
     * @return the visitor signals for this login attempt, never null
     */
    VisitorCheck check(String requestId);
}
