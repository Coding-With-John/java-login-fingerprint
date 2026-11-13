package com.codingwithjohn.login.fingerprint;

import com.fingerprint.v4.api.FingerprintApi;
import com.fingerprint.v4.model.BotResult;
import com.fingerprint.v4.model.Event;
import com.fingerprint.v4.model.IdentificationConfidence;
import com.fingerprint.v4.sdk.ApiClient;
import com.fingerprint.v4.sdk.ApiException;
import com.fingerprint.v4.sdk.Configuration;
import com.fingerprint.v4.sdk.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Real Fingerprint lookup using the Fingerprint Java Server SDK v8
 * (Maven: com.github.fingerprintjs:java-sdk:v8.8.0, Server API v4).
 *
 * Flow: the frontend sends the event id with the login POST, and this
 * service calls {@code getEvent(eventId)} with the secret API key to get
 * the trusted visitor id plus Smart Signals (bot, VPN, incognito, proxy,
 * suspect score). Only the event id is sent to Fingerprint, never the
 * username or password.
 *
 * The SDK call shape here was verified against the published v8.8.0 jar
 * (Configuration.getDefaultApiClient, FingerprintApi.getEvent, and the
 * Event model getters). What still needs confirming at the kick-off call
 * with Fingerprint's tech team:
 *
 * <ul>
 *   <li>TODO: which Smart Signals are active on our plan. Bot detection
 *       needs Pro Plus or higher on some plans, so {@code getBot()} may
 *       come back NOT_DETECTED until that is enabled.</li>
 *   <li>TODO: how a failed lookup should behave in production. This
 *       starter fails OPEN (lets the login proceed without signals) and
 *       logs a warning, so a Fingerprint outage does not lock everyone
 *       out. Failing closed may be the right call for the real video.</li>
 *   <li>TODO: the data region. This starter uses Region.GLOBAL; confirm
 *       whether the account uses the EU or Asia region instead.</li>
 * </ul>
 *
 * Not a Spring bean on its own: {@link FingerprintConfig} creates it when
 * a secret key is configured.
 */
public class RealFingerprintService implements FingerprintService {

    private static final Logger log = LoggerFactory.getLogger(RealFingerprintService.class);

    private final FingerprintApi api;

    public RealFingerprintService(String secretKey) {
        ApiClient client = Configuration.getDefaultApiClient(secretKey, Region.GLOBAL);
        this.api = new FingerprintApi(client);
    }

    @Override
    public VisitorCheck check(String requestId) {
        if (requestId == null || requestId.isBlank() || requestId.startsWith("stub-")) {
            return VisitorCheck.unknown("No Fingerprint event id sent, skipping lookup");
        }
        try {
            Event event = api.getEvent(requestId);

            String visitorId = "unknown";
            Double confidenceScore = null;
            if (event.getIdentification() != null) {
                visitorId = event.getIdentification().getVisitorId();
                IdentificationConfidence confidence = event.getIdentification().getConfidence();
                if (confidence != null) {
                    confidenceScore = confidence.getScore();
                }
            }

            boolean bot = event.getBot() == BotResult.BAD;
            boolean vpn = Boolean.TRUE.equals(event.getVpn());
            boolean incognito = Boolean.TRUE.equals(event.getIncognito());
            boolean proxy = Boolean.TRUE.equals(event.getProxy());
            int suspectScore = event.getSuspectScore() != null ? event.getSuspectScore() : 0;

            String detail = "Fingerprint event for " + visitorId
                    + " (confidence " + confidenceScore + ")"
                    + (bot ? " [BOT]" : "")
                    + (vpn ? " [VPN]" : "")
                    + (incognito ? " [incognito]" : "")
                    + (proxy ? " [proxy]" : "");
            log.debug("Fingerprint check: {}", detail);
            return new VisitorCheck(visitorId, bot, vpn, incognito, proxy, suspectScore, false, detail);
        } catch (ApiException e) {
            // TODO: confirm fail-open vs fail-closed with Fingerprint (see class Javadoc).
            log.warn("Fingerprint lookup failed for event {}: {}", requestId, e.getMessage());
            return VisitorCheck.unknown("Fingerprint lookup failed, allowed without signals");
        }
    }
}
