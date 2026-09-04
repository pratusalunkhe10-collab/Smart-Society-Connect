package com.smartsocietyconnect.auth.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing users in the Smart Society Connect application.
 *
 * <p>Maps to the {@code users} table in the database.
 *
 * <p>This entity stores authentication and profile-related
 * information for all system users.
 *
 * <p>Note: {@code Serializable} is intentionally omitted — it is not
 * required by the JPA specification for standard Spring Boot applications,
 * and combining it with Lombok's {@code @Builder} can silently suppress
 * inline field defaults.
 *
 * <p>Note: {@code @Data} and {@code @EqualsAndHashCode} are intentionally
 * avoided to prevent issues with JPA lazy-loading proxies and
 * bidirectional relationship cycles.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * Primary key for the user.
     *
     * <p>Maps to {@code user_id} column (INT UNSIGNED, AUTO_INCREMENT).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    /**
     * User's first name.
     *
     * <p>Maps to {@code first_name} column (VARCHAR(30), NOT NULL).
     */
    @Column(name = "first_name", nullable = false, length = 30)
    private String firstName;

    /**
     * User's last name.
     *
     * <p>Maps to {@code last_name} column (VARCHAR(30), nullable).
     */
    @Column(name = "last_name", length = 30)
    private String lastName;

    /**
     * Unique email address used for authentication.
     *
     * <p>Maps to {@code email} column (VARCHAR(100), NOT NULL, UNIQUE).
     */
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    /**
     * User's 10-digit mobile number.
     *
     * <p>Maps to {@code mobile} column (CHAR(10), NOT NULL, UNIQUE).
     * Fixed-length {@code CHAR} is more appropriate than {@code VARCHAR}
     * for a known fixed-size value like a 10-digit mobile number.
     */
    @Column(name = "mobile", nullable = false, unique = true, columnDefinition = "CHAR(10)")
    private String mobile;

    /**
     * BCrypt-encrypted password hash.
     *
     * <p>Maps to {@code password_hash} column (VARCHAR(255), NOT NULL).
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Profile image path or URL.
     *
     * <p>Maps to {@code profile_image} column (VARCHAR(255), nullable).
     */
    @Column(name = "profile_image", length = 255)
    private String profileImage;

    /**
     * Indicates whether the account is active.
     *
     * <p>Maps to {@code is_active} column (TINYINT(1), default TRUE).
     *
     * <p>{@code @Builder.Default} ensures this value is honoured
     * when constructing via the Lombok builder; without it,
     * {@code @Builder} silently sets the field to {@code null}.
     */
    @Builder.Default
    @Column(name = "is_active",  nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean isActive = true;

    /**
     * Indicates whether the account email has been verified.
     *
     * <p>Maps to {@code is_verified} column (TINYINT(1), default FALSE).
     *
     * <p>{@code @Builder.Default} ensures this value is honoured
     * when constructing via the Lombok builder; without it,
     * {@code @Builder} silently sets the field to {@code null}.
     */
    @Builder.Default
    @Column(name = "is_verified",  nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean isVerified = false;

    /**
     * Admin decision after email verification: PENDING, APPROVED, or REJECTED.
     * A verified account cannot sign in until it is APPROVED.
     */
    @Builder.Default
    @Column(name = "approval_status", nullable = false, length = 20)
    private String approvalStatus = "PENDING";

    /**
     * Timestamp when the user record was created.
     *
     * <p>Maps to {@code created_at} column. Set automatically by
     * Hibernate on insert; never updated thereafter.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the user record was last updated.
     *
     * <p>Maps to {@code updated_at} column. Updated automatically
     * by Hibernate on every merge/update operation.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
