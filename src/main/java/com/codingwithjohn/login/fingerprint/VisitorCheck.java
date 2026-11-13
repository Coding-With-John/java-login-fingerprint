package com.codingwithjohn.login.fingerprint;

/**
 * The Fingerprint signals for one login attempt, reduced to what the login
 * gate actually decides on. Fingerprint identifies devices and returns
 * signals; our code makes the allow / step-up / block call.
 *
 * @param visitorId     stable id for the visitor's browser/device; "unknown"
 *                      when the lookup could not identify one
 * @param botDetected   true when Fingerprint flags the visit as automated
 * @param vpnDetected   true when the visit looks like it comes via a VPN
 * @param incognito     true when the visit is in a private/incognito window
 * @param proxyDetected true when the visit comes via a proxy
 * @param suspectScore  Fingerprint's 0-100 suspect score, or 0 when unavailable
 * @param stub          true when these signals were simulated (no API keys)
 * @param detail        one-line human-readable summary, for logs and the demo
 */
public record VisitorCheck(
        String visitorId,
        boolean botDetected,
        boolean vpnDetected,
        boolean incognito,
        boolean proxyDetected,
        int suspectScore,
        boolean stub,
        String detail
) {
    /** Fallback used when the lookup fails or no usable id was sent. */
    public static VisitorCheck unknown(String detail) {
        return new VisitorCheck("unknown", false, false, false, false, 0, false, detail);
    }
}
