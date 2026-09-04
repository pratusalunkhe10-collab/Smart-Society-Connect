package com.smartsocietyconnect.auth.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing active and historical user login sessions.
 *
 * <p>Maps to the {@code user_sessions} table in the database.
 *
 * <p>This entity stores:
 * <ul>
 *     <li>JWT tokens</li>
 *     <li>Login timestamps</li>
 *     <li>Logout timestamps</li>
 *     <li>IP addresses</li>
 *     <li>Device/browser information</li>
 * </ul>
 *
 * <p>Used for:
 * <ul>
 *     <li>Session management</li>
 *     <li>Audit logging</li>
 *     <li>Security monitoring</li>
 *     <li>Multi-device login tracking</li>
 * </ul>
 *
 * <p>Note: {@code FetchType.LAZY} is used to optimize performance.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {

    /**
     * Primary key for session record.
     *
     * <p>Maps to {@code session_id} column
     * (INT UNSIGNED, AUTO_INCREMENT).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Integer sessionId;

    /**
     * User associated with this session.
     *
     * <p>Maps to {@code user_id} foreign key column.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * JWT token issued during authentication.
     *
     * <p>Maps to {@code jwt_token} column.
     */
    @Column(name = "jwt_token", nullable = false, length = 500)
    private String jwtToken;

    /**
     * IP address from which login occurred.
     *
     * <p>Supports both IPv4 and IPv6.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Device/browser information.
     *
     * <p>Example:
     * Chrome on Windows 11
     */
    @Column(name = "device_info", length = 150)
    private String deviceInfo;

    /**
     * Timestamp when user logged in.
     *
     * <p>Automatically generated during insert.
     */
    @CreationTimestamp
    @Column(name = "login_time", updatable = false)
    private LocalDateTime loginTime;

    /**
     * Timestamp when user logged out.
     *
     * <p>Null until logout occurs.
     */
    @Column(name = "logout_time")
    private LocalDateTime logoutTime;

    /**
     * Indicates whether the session has been logged out.
     *
     * <p>Used for token invalidation and session tracking.
     */
    @Builder.Default
    @Column(
        name = "is_logged_out",
        nullable = false,
        columnDefinition = "TINYINT(1) DEFAULT 0"
    )
    private Boolean isLoggedOut = false;

}