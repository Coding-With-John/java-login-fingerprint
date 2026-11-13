package com.codingwithjohn.login.fingerprint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simulates Fingerprint signals so the app runs end to end with no API keys.
 *
 * This is a PLACEHOLDER, not real device intelligence. The mapping is
 * deliberately simple and deterministic so the demo is repeatable:
 *
 * <ul>
 *   <li>The visitor id is derived from the request id, so every login that
 *       sends the same request id looks like the same device. The demo
 *       script relies on this to simulate one attacker device.</li>
 *   <li>A request id containing "bot" simulates a bot visit.</li>
 *   <li>A request id containing "vpn" simulates a VPN visit.</li>
 *   <li>Anything else simulates a clean, ordinary visit.</li>
 * </ul>
 *
 * Not a Spring bean on its own: {@link FingerprintConfig} creates it when no
 * secret key is configured.
 */
public class StubFingerprintService implements FingerprintService {

    private static final Logger log = LoggerFactory.getLogger(StubFingerprintService.class);

    @Override
    public VisitorCheck check(String requestId) {
        String id = (requestId == null || requestId.isBlank()) ? "stub-no-id" : requestId;
        // Stable per request id, so the demo script's repeated logins count
        // as one device. floorMod keeps the number positive and short.
        String visitorId = "stub-visitor-" + Math.floorMod(id.hashCode(), 100000);
        boolean bot = id.toLowerCase().contains("bot");
        boolean vpn = id.toLowerCase().contains("vpn");
        String detail = "STUB signals for " + visitorId
                + (bot ? " [BOT]" : "") + (vpn ? " [VPN]" : "");
        log.debug("Stub Fingerprint check: {}", detail);
        return new VisitorCheck(visitorId, bot, vpn, false, false, bot ? 90 : 0, true, detail);
    }
}
