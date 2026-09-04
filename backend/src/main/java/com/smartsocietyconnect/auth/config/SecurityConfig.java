package com.smartsocietyconnect.auth.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.smartsocietyconnect.auth.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

/**
 * Security configuration for Smart Society Connect.
 *
 * <p>Configures:
 * <ul>
 *     <li>Password encoding</li>
 *     <li>Authentication manager</li>
 *     <li>CORS configuration</li>
 *     <li>JWT-based stateless authentication</li>
 *     <li>Role-based API authorization</li>
 * </ul>
 *
 * <p>This project stores authorities as plain role names such as
 * {@code ADMIN}, {@code RESIDENT}, {@code SECRETARY}, {@code SECURITY},
 * and {@code ACCOUNTANT}. Therefore this class uses
 * {@code hasAuthority(...)} and {@code hasAnyAuthority(...)} instead of
 * {@code hasRole(...)}.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    // ==========================================================================
    // 1. Password Encoder Bean
    // ==========================================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ==========================================================================
    // 2. Authentication Manager Bean
    // ==========================================================================

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }

    // ==========================================================================
    // 3. CORS Configuration
    // ==========================================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .toList()
        );

        configuration.setAllowedMethods(
                Arrays.asList(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "PATCH",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }

    // ==========================================================================
    // 4. Security Filter Chain
    // ==========================================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // ======================================================
                        // Public APIs
                        // ======================================================

                        .requestMatchers("/api/auth/logout", "/api/auth/profile")
                        .authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/auth/profile/image")
                        .authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/auth/profile/image/*")
                        .permitAll()

                        .requestMatchers("/api/health")
                        .permitAll()

                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**"
                        ).permitAll()

                        // ======================================================
                        // Dashboard APIs
                        // ======================================================

                        .requestMatchers(
                                "/api/dashboard/admin",
                                "/api/dashboard/admin/**"
                        )
                        .hasAnyAuthority("ADMIN", "SECRETARY")

                        .requestMatchers(
                                "/api/dashboard/resident",
                                "/api/dashboard/resident/**"
                        )
                        .hasAuthority("RESIDENT")

                        .requestMatchers(
                                "/api/dashboard/security",
                                "/api/dashboard/security/**"
                        )
                        .hasAuthority("SECURITY")

                        .requestMatchers(
                                "/api/dashboard/accountant",
                                "/api/dashboard/accountant/**"
                        )
                        .hasAuthority("ACCOUNTANT")

                        // ======================================================
                        // Meeting Attendance APIs
                        // Put these before general GET /api/meetings/** rules.
                        // ======================================================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/meetings/*/attendance/*"
                        )
                        .hasAuthority("RESIDENT")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/meetings/*/attendance/*"
                        )
                        .hasAuthority("RESIDENT")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/meetings/*/attendance/*"
                        )
                        .hasAuthority("RESIDENT")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/meetings/attendance/resident/*"
                        )
                        .hasAnyAuthority(
                                "ADMIN",
                                "SECRETARY",
                                "RESIDENT"
                        )

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/meetings/*/attendance/resident/*"
                        )
                        .hasAnyAuthority("ADMIN", "SECRETARY", "RESIDENT")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/meetings/*/attendance/**"
                        )
                        .hasAnyAuthority(
                                "ADMIN",
                                "SECRETARY",
                                "SECURITY"
                        )

                        // ======================================================
                        // Meeting Management APIs
                        // ======================================================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/meetings"
                        )
                        .hasAnyAuthority("ADMIN", "SECRETARY")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/meetings/*"
                        )
                        .hasAnyAuthority("ADMIN", "SECRETARY")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/meetings/*/status"
                        )
                        .hasAnyAuthority("ADMIN", "SECRETARY")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/meetings/*/cancel"
                        )
                        .hasAnyAuthority("ADMIN", "SECRETARY")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/meetings/*"
                        )
                        .hasAuthority("ADMIN")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/meetings/*/minutes"
                        )
                        .hasAnyAuthority("ADMIN", "SECRETARY")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/meetings/*/minutes"
                        )
                        .hasAnyAuthority(
                                "ADMIN",
                                "SECRETARY",
                                "RESIDENT",
                                "SECURITY",
                                "ACCOUNTANT"
                        )

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/meetings/**"
                        )
                        .hasAnyAuthority(
                                "ADMIN",
                                "SECRETARY",
                                "RESIDENT",
                                "SECURITY",
                                "ACCOUNTANT"
                        )

                        // ======================================================
                        // Resident APIs
                        // ======================================================

                        .requestMatchers(HttpMethod.GET, "/api/residents/user/*")
                        .hasAnyAuthority("ADMIN", "SECRETARY", "RESIDENT")

                        // Security needs a single-resident lookup while registering
                        // a visitor. Keep this before the broader residents rule so
                        // guards cannot list, edit, or delete resident records.
                        .requestMatchers(HttpMethod.GET, "/api/residents/*")
                        .hasAnyAuthority("ADMIN", "SECRETARY", "SECURITY", "ACCOUNTANT")

                        .requestMatchers(HttpMethod.GET, "/api/residents/**")
                        .hasAnyAuthority("ADMIN", "SECRETARY")

                        .requestMatchers(HttpMethod.POST, "/api/residents")
                        .hasAnyAuthority("ADMIN", "SECRETARY")

                        .requestMatchers(HttpMethod.PUT, "/api/residents/*")
                        .hasAnyAuthority("ADMIN", "SECRETARY")

                        .requestMatchers(HttpMethod.DELETE, "/api/residents/*")
                        .hasAnyAuthority("ADMIN", "SECRETARY")

                        // ======================================================
                        // Visitor APIs
                        // ======================================================

                        .requestMatchers(HttpMethod.PUT, "/api/visitors/*/approve")
                        .hasAnyAuthority("ADMIN", "RESIDENT", "SECRETARY")

                        .requestMatchers(HttpMethod.PUT, "/api/visitors/*/check-in", "/api/visitors/*/check-out")
                        .hasAnyAuthority("ADMIN", "SECURITY")

                        .requestMatchers(HttpMethod.GET, "/api/visitors/**")
                        .hasAnyAuthority("ADMIN", "RESIDENT", "SECURITY", "SECRETARY")

                        .requestMatchers(HttpMethod.POST, "/api/visitors")
                        .hasAnyAuthority("ADMIN", "RESIDENT", "SECURITY", "SECRETARY")

                        .requestMatchers(HttpMethod.DELETE, "/api/visitors/*")
                        .hasAnyAuthority("ADMIN", "SECRETARY")

                        // ======================================================
                        // Complaint APIs
                        // ======================================================

                        .requestMatchers(HttpMethod.PATCH, "/api/complaints/*/status")
                        .hasAnyAuthority("ADMIN", "SECRETARY")

                        .requestMatchers(HttpMethod.GET, "/api/complaints/**")
                        .hasAnyAuthority("ADMIN", "RESIDENT", "SECRETARY")

                        .requestMatchers(HttpMethod.POST, "/api/complaints")
                        .hasAnyAuthority("ADMIN", "RESIDENT", "SECRETARY")

                        .requestMatchers(HttpMethod.PUT, "/api/complaints/*")
                        .hasAnyAuthority("ADMIN", "RESIDENT", "SECRETARY")

                        .requestMatchers(HttpMethod.DELETE, "/api/complaints/*")
                        .hasAnyAuthority("ADMIN", "RESIDENT", "SECRETARY")

                        // ======================================================
                        // Billing and payment APIs
                        // ======================================================

                        .requestMatchers(HttpMethod.POST, "/api/razorpay/webhook")
                        .permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/billing/**", "/api/payments/**")
                        .hasAnyAuthority("ADMIN", "RESIDENT", "ACCOUNTANT", "SECRETARY")

                        .requestMatchers(HttpMethod.POST, "/api/razorpay/**")
                        .hasAnyAuthority("ADMIN", "RESIDENT", "ACCOUNTANT", "SECRETARY")

                        .requestMatchers(HttpMethod.POST, "/api/billing")
                        .hasAnyAuthority("ADMIN", "ACCOUNTANT", "SECRETARY")

                        .requestMatchers(HttpMethod.PUT, "/api/billing/*")
                        .hasAnyAuthority("ADMIN", "ACCOUNTANT", "SECRETARY")

                        .requestMatchers(HttpMethod.DELETE, "/api/billing/*")
                        .hasAnyAuthority("ADMIN", "ACCOUNTANT", "SECRETARY")

                        .requestMatchers(HttpMethod.POST, "/api/payments")
                        .hasAnyAuthority("ADMIN", "ACCOUNTANT", "SECRETARY")

                        // ======================================================
                        // All Other APIs
                        // ======================================================

                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
