package com.smartsocietyconnect.auth.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.smartsocietyconnect.auth.exception.AuthException;
import com.smartsocietyconnect.auth.repository.UserSessionRepository;
import com.smartsocietyconnect.auth.util.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT Authentication Filter — runs once per HTTP request.
 *
 * <p>Intercepts every request, extracts the JWT from the
 * {@code Authorization: Bearer <token>} header, validates it,
 * and populates the Spring Security context if valid.
 *
 * <p>Relies on {@link JwtUtil} for all token operations:
 * <ul>
 *     <li>{@link JwtUtil#extractUsername(String)} — reads subject claim</li>
 *     <li>{@link JwtUtil#validateToken(String)}   — verifies signature + expiry</li>
 * </ul>
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil                  jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserSessionRepository    userSessionRepository;

    /** Explicit constructor keeps this security-critical class independent of IDE Lombok settings. */
    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            CustomUserDetailsService userDetailsService,
            UserSessionRepository userSessionRepository
    ) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userSessionRepository = userSessionRepository;
    }

    /**
     * Core filter logic — extracts, validates, and authenticates JWT.
     *
     * <p>Flow:
     * <ol>
     *   <li>Read {@code Authorization} header</li>
     *   <li>Strip {@code Bearer } prefix and extract raw token</li>
     *   <li>Extract username (email) via {@link JwtUtil#extractUsername(String)}</li>
     *   <li>Load {@link UserDetails} via {@link CustomUserDetailsService}</li>
     *   <li>Validate token via {@link JwtUtil#validateToken(String)}</li>
     *   <li>Set authentication in {@link SecurityContextHolder}</li>
     * </ol>
     *
     * @param request     incoming HTTP request
     * @param response    outgoing HTTP response
     * @param filterChain remaining filter chain
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,
            HttpServletResponse response,
            FilterChain         filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        String jwtToken = null;
        String username = null;

        // =====================================================================
        // STEP 1 — Extract JWT token and username from Authorization header
        // =====================================================================

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            jwtToken = authHeader.substring(7); // strip "Bearer " prefix

            try {
                // ✅ extractUsername() — correct method name from JwtUtil
                username = jwtUtil.extractUsername(jwtToken);

            } catch (AuthException e) {
                // Token is malformed or has invalid signature — skip authentication
                log.error("JWT extraction failed: {}", e.getMessage());

            } catch (Exception e) {
                log.error("Unexpected JWT extraction error: {}", e.getMessage());
            }
        }

        // =====================================================================
        // STEP 2 — Validate token and populate Spring Security context
        // =====================================================================

        if (username != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            try {
                // ✅ validateToken(token) — single argument, correct from JwtUtil
                boolean hasActiveSession = userSessionRepository.findByJwtToken(jwtToken)
                        .map(session -> !Boolean.TRUE.equals(session.getIsLoggedOut()))
                        .orElse(false);

                if (jwtUtil.validateToken(jwtToken) && hasActiveSession) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("JWT authenticated successfully for user: {}", username);
                } else if (!hasActiveSession) {
                    log.warn("JWT session is absent or logged out for user: {}", username);
                }

            } catch (AuthException e) {
                // validateToken() throws AuthException for expired/tampered/malformed tokens
                // Do NOT rethrow — just skip setting auth so request proceeds as anonymous
                log.warn("JWT validation failed for user {}: {}", username, e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
