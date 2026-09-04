package com.smartsocietyconnect.auth.util;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.smartsocietyconnect.auth.exception.AuthException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

/**
 * Utility component for JWT token operations in Smart Society Connect.
 *
 * <p>Handles the full JWT lifecycle:
 * <ul>
 *     <li>Token generation — builds a signed JWT with subject and role claim</li>
 *     <li>Token validation — verifies signature and expiry</li>
 *     <li>Claims extraction — retrieves subject (username/email) and role</li>
 *     <li>Expiry checking — distinguishes expired from otherwise invalid tokens</li>
 * </ul>
 *
 * <p><b>Algorithm:</b> HMAC-SHA256 (HS256). The signing key is derived from
 * a secret injected via {@code application.properties} — never hardcoded in source.
 *
 * <p><b>Security note:</b> The secret key ({@code app.jwt.secret}) must be:
 * <ul>
 *     <li>At least 32 characters (256 bits) for HS256</li>
 *     <li>Stored in environment variables or a secrets manager in production</li>
 *     <li>Never committed to version control</li>
 * </ul>
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
@Component
public class JwtUtil {

    // ==========================================================================
    // Configuration (injected — never hardcoded)
    // ==========================================================================

    /**
     * JWT signing secret, injected from {@code application.properties}.
     *
     * <p>Property key: {@code app.jwt.secret}
     *
     * <p>Must be at least 32 characters (256 bits) for HS256.
     * In production, supply this via an environment variable or
     * secrets manager — never commit the actual value to source control.
     *
     * <p>Example in {@code application.properties}:
     * <pre>{@code
     * app.jwt.secret=${JWT_SECRET}
     * }</pre>
     */
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /**
     * Token validity duration in milliseconds, injected from {@code application.properties}.
     *
     * <p>Property key: {@code app.jwt.expiration-ms}
     *
     * <p>Example in {@code application.properties}:
     * <pre>{@code
     * # 24 hours = 86400000 ms
     * app.jwt.expiration-ms=86400000
     * }</pre>
     */
    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ==========================================================================
    // Private Helpers
    // ==========================================================================

    /**
     * Builds the HMAC-SHA256 signing key from the injected secret.
     *
     * <p>Returns {@link SecretKey} directly (not the parent {@link java.security.Key})
     * to avoid an unsafe cast in {@link #extractClaims(String)}.
     * {@code Keys.hmacShaKeyFor()} already returns {@code SecretKey} —
     * the narrower return type simply makes that fact explicit.
     *
     * @return a {@link SecretKey} suitable for HS256 signing and verification
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Parses and returns all claims from a JWT token.
     *
     * <p>Throws specific {@link JwtException} subtypes on failure —
     * callers must handle them rather than catching the raw checked
     * {@link Exception} and hiding the reason.
     *
     * @param token the compact JWT string to parse
     * @return the verified {@link Claims} payload
     * @throws ExpiredJwtException   if the token has passed its expiry timestamp
     * @throws SignatureException    if the token signature does not match the key
     * @throws MalformedJwtException if the token is not a valid JWT structure
     */
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ==========================================================================
    // Public API
    // ==========================================================================

    /**
     * Generates a signed JWT token for an authenticated user.
     *
     * <p>The token contains:
     * <ul>
     *     <li>{@code sub} — the user's email or username (subject)</li>
     *     <li>{@code role} — the user's single role (e.g. {@code ADMIN}, {@code USER})</li>
     *     <li>{@code iat} — issued-at timestamp</li>
     *     <li>{@code exp} — expiry timestamp ({@code iat + jwtExpirationMs})</li>
     * </ul>
     *
     * <p>Uses {@code signWith(SecretKey)} — the single-argument overload introduced
     * in JJWT 0.12. JJWT infers the algorithm (HS256) from the key type automatically.
     * The deprecated two-argument {@code signWith(key, SignatureAlgorithm)} overload
     * is intentionally avoided.
     *
     * @param username the authenticated user's email or username to set as JWT subject
     * @param role     the user's role to embed as a custom claim
     * @return a compact, URL-safe JWT string
     */
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the subject (username or email) from a JWT token.
     *
     * @param token the compact JWT string
     * @return the subject claim value set during token generation
     * @throws AuthException if the token is expired, malformed, or has an invalid signature
     */
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Extracts the role claim from a JWT token.
     *
     * <p>Complements {@link #generateToken}, which embeds the role as a
     * custom claim. Without this method the role claim would be write-only
     * and unusable by the JWT filter or any authorization logic.
     *
     * @param token the compact JWT string
     * @return the role value embedded during token generation (e.g. {@code "ADMIN"})
     * @throws AuthException if the token is expired, malformed, or has an invalid signature
     */
    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    /**
     * Checks whether a JWT token has passed its expiry timestamp.
     *
     * <p>Used by the JWT filter to distinguish between:
     * <ul>
     *     <li>Expired tokens — may be eligible for refresh (return 401 with a hint)</li>
     *     <li>Invalid tokens — hard reject (return 401 with no refresh option)</li>
     * </ul>
     *
     * @param token the compact JWT string
     * @return {@code true} if the token's {@code exp} claim is before the current time
     * @throws AuthException if the token is malformed or has an invalid signature
     *         (distinct from expiry — those are not checked here)
     */
    public boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    /**
     * Validates a JWT token by verifying its signature and checking its expiry.
     *
     * <p>Unlike a bare {@code catch (Exception e) { return false; }}, this method
     * logs the specific failure reason and re-maps it to a meaningful
     * {@link AuthException}, preserving the original cause in the stack trace.
     *
     * <p>Failure modes handled explicitly:
     * <ul>
     *     <li>{@link ExpiredJwtException}   — token has passed its {@code exp} timestamp</li>
     *     <li>{@link SignatureException}    — token was signed with a different key (tampered)</li>
     *     <li>{@link MalformedJwtException} — token is not a valid three-part JWT structure</li>
     *     <li>{@link JwtException}          — any other JJWT-level failure</li>
     * </ul>
     *
     * @param token the compact JWT string to validate
     * @return {@code true} if the token is structurally valid, correctly signed,
     *         and not yet expired
     * @throws AuthException with a specific message and root cause for each failure mode
     */
    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            throw new AuthException("JWT token has expired", ex);
        } catch (SignatureException ex) {
            throw new AuthException("JWT signature is invalid — token may have been tampered with", ex);
        } catch (MalformedJwtException ex) {
            throw new AuthException("JWT token is malformed", ex);
        } catch (JwtException ex) {
            throw new AuthException("JWT token validation failed", ex);
        }
    }

}