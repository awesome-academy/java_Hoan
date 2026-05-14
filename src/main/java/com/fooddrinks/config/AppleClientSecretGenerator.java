package com.fooddrinks.config;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;

/**
 * Generates the Apple client_secret — a JWT signed with ES256 using your
 * Apple .p8 private key. Apple requires this instead of a static client secret.
 *
 * Token is valid for 180 days (Apple maximum is 6 months).
 * Regenerated on each application startup.
 *
 * Required config (application.yml or env vars):
 * app.apple.team-id — 10-character Team ID from Apple Developer portal
 * app.apple.key-id — Key ID from the .p8 file header
 * app.apple.private-key — PEM content of the .p8 key (PKCS8 EC key, no header
 * line needed)
 * spring.security.oauth2.client.registration.apple.client-id — your Services ID
 *
 * Reference:
 * https://developer.apple.com/documentation/sign_in_with_apple/generate_and_validate_tokens
 */
@Slf4j
@Component
public class AppleClientSecretGenerator {

    private static final String APPLE_AUDIENCE = "https://appleid.apple.com";
    private static final long VALIDITY_DAYS = 180;

    @Value("${app.apple.team-id:}")
    private String teamId;

    @Value("${app.apple.key-id:}")
    private String keyId;

    @Value("${app.apple.private-key:}")
    private String privateKeyPem;

    @Value("${spring.security.oauth2.client.registration.apple.client-id:}")
    private String clientId;

    /**
     * Returns true if all Apple credentials are present and appear non-placeholder.
     */
    public boolean isConfigured() {
        return !teamId.isBlank() && !teamId.startsWith("YOUR_")
                && !keyId.isBlank() && !keyId.startsWith("YOUR_")
                && !privateKeyPem.isBlank() && !privateKeyPem.startsWith("YOUR_")
                && !clientId.isBlank() && !clientId.startsWith("YOUR_");
    }

    /**
     * Generates a signed JWT for use as Apple's client_secret.
     * Returns a placeholder string when Apple credentials are not configured,
     * so the app starts normally and other OAuth2 providers still work.
     */
    public String generate() {
        if (!isConfigured()) {
            log.warn("Apple OAuth2 credentials not configured — Apple Sign In will not work. "
                    + "Set app.apple.team-id, app.apple.key-id, app.apple.private-key, "
                    + "and spring.security.oauth2.client.registration.apple.client-id.");
            return "PLACEHOLDER_APPLE_CLIENT_SECRET_NOT_CONFIGURED";
        }
        try {
            PrivateKey pk = loadPrivateKey();
            Instant now = Instant.now();
            return Jwts.builder()
                    .header().add(Map.of("kid", keyId)).and()
                    .issuer(teamId)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plus(VALIDITY_DAYS, ChronoUnit.DAYS)))
                    .audience().add(APPLE_AUDIENCE).and()
                    .subject(clientId)
                    .signWith(pk, Jwts.SIG.ES256)
                    .compact();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to generate Apple client_secret JWT. "
                            + "Verify that app.apple.private-key is a valid PKCS8 EC private key.",
                    e);
        }
    }

    /**
     * Parses the PEM-encoded EC private key.
     * Accepts either raw base64 or a full PEM block (with -----BEGIN/END PRIVATE
     * KEY----- headers).
     */
    private PrivateKey loadPrivateKey() throws Exception {
        String pem = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("EC")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }
}
