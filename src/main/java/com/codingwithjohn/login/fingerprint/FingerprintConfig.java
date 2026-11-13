package com.codingwithjohn.login.fingerprint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Picks the Fingerprint implementation: the real SDK-backed service when a
 * secret key is configured, otherwise the stub that simulates signals.
 *
 * Configure with environment variables (never commit real keys):
 * FINGERPRINT_SECRET_KEY for the server lookup, FINGERPRINT_PUBLIC_KEY for
 * the JavaScript agent in the browser.
 */
@Configuration
public class FingerprintConfig {

    private static final Logger log = LoggerFactory.getLogger(FingerprintConfig.class);

    @Bean
    public FingerprintService fingerprintService(
            @Value("${fingerprint.secret-key:}") String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            log.info("No Fingerprint secret key configured, using StubFingerprintService");
            return new StubFingerprintService();
        }
        log.info("Fingerprint secret key configured, using RealFingerprintService");
        return new RealFingerprintService(secretKey);
    }
}
