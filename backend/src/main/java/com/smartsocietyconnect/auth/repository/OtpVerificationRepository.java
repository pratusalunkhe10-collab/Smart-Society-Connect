package com.smartsocietyconnect.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartsocietyconnect.auth.entity.OtpVerification;
import com.smartsocietyconnect.auth.entity.User;

/**
 * Repository interface for {@link OtpVerification} entity.
 *
 * <p>Provides CRUD and query operations for the {@code otp_verification} table.
 *
 * <p>Extends {@link JpaRepository} with generic types:
 * <ul>
 *     <li>{@link OtpVerification} — the entity type</li>
 *     <li>{@link Integer}         — the primary key type, matching {@code otp_id} (INT)</li>
 * </ul>
 *
 * <p>Spring Data JPA automatically provides standard operations including:
 * <ul>
 *     <li>{@code save(OtpVerification)}    — insert a new OTP record</li>
 *     <li>{@code findById(Integer)}        — lookup by primary key</li>
 *     <li>{@code deleteById(Integer)}      — remove by primary key</li>
 *     <li>{@code count()}                  — total record count</li>
 * </ul>
 *
 * <p>Used during:
 * <ul>
 *     <li>Registration      — generate and validate email/mobile OTP</li>
 *     <li>Forgot password   — generate and validate password-reset OTP</li>
 *     <li>OTP verification  — load and mark OTP as used after successful validation</li>
 *     <li>Scheduled cleanup — purge expired OTP records to keep the table lean</li>
 * </ul>
 *
 * <p>Note: {@code @Repository} is technically redundant on {@link JpaRepository}
 * extensions, but is included explicitly as enterprise convention — it
 * documents intent clearly and enables Spring's persistence exception
 * translation layer.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see OtpVerification
 */
@Repository
public interface OtpVerificationRepository
        extends JpaRepository<OtpVerification, Integer> {

    // ==========================================================================
    // Lookup Methods
    // ==========================================================================

    /**
     * Finds the most recently created OTP record for a given user,
     * regardless of its used or expiry status.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code
     * SELECT * FROM otp_verification
     * WHERE user_id = ?
     * ORDER BY created_at DESC
     * LIMIT 1
     * }</pre>
     *
     * <p><b>Use with caution</b> — this returns the latest OTP even if it is
     * already used or expired. For OTP validation flows, prefer
     * {@link #findTopByUserAndIsUsedFalseAndExpiryTimeAfterOrderByCreatedAtDesc}
     * which enforces both constraints atomically at the DB level.
     *
     * <p>This method is appropriate for audit/logging purposes or to check
     * whether any OTP has been issued to a user recently (e.g. rate limiting).
     *
     * @param user the {@link User} entity to look up the latest OTP for
     * @return an {@link Optional} containing the most recent {@link OtpVerification},
     *         or {@link Optional#empty()} if no OTP has ever been issued for this user
     */
    Optional<OtpVerification> findTopByUserOrderByCreatedAtDesc(User user);

    /**
     * Finds the most recently created, unused, and still-valid OTP for a given user.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code
     * SELECT * FROM otp_verification
     * WHERE user_id     = ?
     *   AND is_used     = 0
     *   AND expiry_time > ?
     * ORDER BY created_at DESC
     * LIMIT 1
     * }</pre>
     *
     * <p><b>This is the primary method for OTP validation flows.</b>
     * It enforces all three required conditions atomically at the database level:
     * <ol>
     *     <li>Belongs to the correct user</li>
     *     <li>Has not already been consumed ({@code is_used = false})</li>
     *     <li>Has not yet expired ({@code expiry_time > now})</li>
     * </ol>
     *
     * <p>Performing these checks in Java after a plain {@code findTopByUser} call
     * creates a race condition window where a second request could consume the
     * same OTP between the load and the check. This query eliminates that window.
     *
     * @param user        the {@link User} entity to validate the OTP for
     * @param currentTime the current timestamp to compare against {@code expiry_time};
     *                    pass {@code LocalDateTime.now()}
     * @return an {@link Optional} containing the valid, unused {@link OtpVerification},
     *         or {@link Optional#empty()} if no valid OTP exists for this user
     */
    Optional<OtpVerification> findTopByUserAndIsUsedFalseAndExpiryTimeAfterOrderByCreatedAtDesc(
            User user,
            LocalDateTime currentTime
    );

    // ==========================================================================
    // Delete Methods
    // ==========================================================================

    /**
     * Deletes all OTP records associated with a specific user.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code DELETE FROM otp_verification WHERE user_id = ?}</pre>
     *
     * <p>Must be called before deleting a {@link User} entity to avoid
     * a foreign key constraint violation. Also useful for resetting a
     * user's OTP history during account recovery flows.
     *
     * @param user the {@link User} entity whose OTP records are to be removed
     */
    void deleteByUser(User user);

    /**
     * Deletes all OTP records whose expiry time is before the given timestamp.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code DELETE FROM otp_verification WHERE expiry_time < ?}</pre>
     *
     * <p>Designed to be called by a scheduled cleanup job (e.g. via
     * {@code @Scheduled}) to purge expired OTP records and prevent unbounded
     * table growth. Recommended schedule: daily or hourly depending on OTP volume.
     *
     * <p>Example usage in a scheduled service:
     * <pre>{@code
     * @Scheduled(cron = "0 0 * * * *") // every hour
     * public void purgeExpiredOtps() {
     *     otpVerificationRepository.deleteByExpiryTimeBefore(LocalDateTime.now());
     * }
     * }</pre>
     *
     * @param cutoff the cutoff timestamp; all records with {@code expiry_time}
     *               before this value will be permanently deleted
     */
    void deleteByExpiryTimeBefore(LocalDateTime cutoff);

}