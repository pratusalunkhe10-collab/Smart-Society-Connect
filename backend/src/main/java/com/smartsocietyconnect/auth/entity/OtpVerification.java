package com.smartsocietyconnect.auth.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing OTP verification records in the Smart Society Connect application.
 *
 * <p>Maps to the {@code otp_verification} table in the database.
 *
 * <p>This entity stores OTPs generated for:
 * <ul>
 *     <li>User registration</li>
 *     <li>Forgot password</li>
 *     <li>Email / mobile verification</li>
 * </ul>
 *
 * <p>Each OTP record belongs to exactly one {@link User}. The association
 * uses {@code FetchType.LAZY} to avoid loading the full user graph on
 * every OTP lookup.
 *
 * <p><b>Design note:</b> {@code @AllArgsConstructor} is intentionally omitted.
 * Combining it with {@code @Builder.Default} causes Lombok to generate a
 * duplicate internal field, producing a compile error or silently ignoring
 * the declared default value. Use the builder or the no-arg constructor
 * with setters instead.
 *
 * <p>Note: {@code @Data} and {@code @EqualsAndHashCode} are intentionally
 * avoided to prevent issues with JPA lazy-loading proxies and
 * bidirectional relationship cycles.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
@Entity
@Table(
    name = "otp_verification",
    indexes = {
        /**
         * Index on user_id for fast OTP lookups per user.
         * Without this, every lookup by user causes a full table scan.
         */
        @Index(name = "idx_otp_user_id", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@Builder
public class OtpVerification {

    /**
     * Primary key for the OTP verification record.
     *
     * <p>Maps to {@code otp_id} column (INT UNSIGNED, AUTO_INCREMENT).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "otp_id")
    private Integer otpId;

    /**
     * User associated with this OTP.
     *
     * <p>Maps to {@code user_id} foreign key column (NOT NULL).
     *
     * <p>{@code FetchType.LAZY} defers loading of the {@link User}
     * entity until explicitly accessed, avoiding unnecessary joins
     * on OTP validation queries.
     *
     * <p>{@code optional = false} aligns the ORM-level constraint with
     * the DB-level {@code NOT NULL} on the FK column. Without this,
     * Hibernate assumes the association is nullable at the ORM level
     * even when the column is NOT NULL.
     *
     * <p>No {@code cascade} is applied — OTP records do not manage
     * the lifecycle of the associated {@link User}.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public OtpVerification(Integer otpId, User user, String otpCode, LocalDateTime expiryTime, Boolean isUsed,
			LocalDateTime createdAt) {
		super();
		this.otpId = otpId;
		this.user = user;
		this.otpCode = otpCode;
		this.expiryTime = expiryTime;
		this.isUsed = isUsed;
		this.createdAt = createdAt;
	}

	/**
     * 6-digit OTP code.
     *
     * <p>Maps to {@code otp_code} column (CHAR(6), NOT NULL).
     * Fixed-length {@code CHAR} is used since OTPs are always exactly 6 digits.
     */
    @Column(name = "otp_code", nullable = false, columnDefinition = "CHAR(6)")
    private String otpCode;

    /**
     * OTP expiry timestamp.
     *
     * <p>Maps to {@code expiry_time} column (DATETIME, NOT NULL).
     * After this timestamp, the OTP must be treated as invalid
     * regardless of its {@code isUsed} state.
     */
    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;

    /**
     * Indicates whether this OTP has already been used.
     *
     * <p>Maps to {@code is_used} column (TINYINT(1), NOT NULL, DEFAULT 0).
     * Prevents OTP reuse attacks by marking codes as consumed after
     * a single successful verification.
     *
     * <p>{@code @Builder.Default} ensures this value is honoured
     * when constructing via the Lombok builder; without it,
     * {@code @Builder} silently sets the field to {@code null}.
     *
     * <p>{@code nullable = false} enforces the NOT NULL constraint
     * at the JPA/DDL level. The {@code columnDefinition} alone is
     * not sufficient for JPA schema generation.
     */
    @Builder.Default
    @Column(
        name    = "is_used",
        nullable = false,
        columnDefinition = "TINYINT(1) DEFAULT 0"
    )
    private Boolean isUsed = false;

    /**
     * Timestamp when this OTP record was created.
     *
     * <p>Maps to {@code created_at} column. Set automatically by
     * Hibernate on insert and never updated thereafter.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

}