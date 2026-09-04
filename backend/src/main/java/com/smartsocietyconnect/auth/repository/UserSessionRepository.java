package com.smartsocietyconnect.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.auth.entity.UserSession;

/**
 * Repository interface for {@link UserSession}.
 *
 * <p>Handles user session and JWT tracking operations.
 *
 * <p>Maps to the {@code user_sessions} table which contains:
 * {@code session_id, user_id, jwt_token, ip_address, device_info,
 * login_time, logout_time, is_logged_out}
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Integer> {

    /**
     * Finds all sessions for a specific user.
     *
     * <p>Derived query:
     * <pre>{@code SELECT * FROM user_sessions WHERE user_id = ?}</pre>
     *
     * @param user user entity
     * @return list of all sessions for the user
     */
    List<UserSession> findByUser(User user);

    /**
     * Finds all sessions for a user filtered by logout status.
     *
     * <p>Derived query:
     * <pre>{@code SELECT * FROM user_sessions WHERE user_id = ? AND is_logged_out = ?}</pre>
     *
     * <p>Pass {@code false} to get active (not yet logged out) sessions.
     * Pass {@code true} to get historical logged-out sessions.
     *
     * <p>Note: Field is {@code isLoggedOut} (maps to {@code is_logged_out} column).
     * The previous {@code findByUserAndIsActive} was incorrect — {@code UserSession}
     * has no {@code isActive} field.
     *
     * @param user       user entity
     * @param isLoggedOut pass {@code false} for active sessions, {@code true} for logged-out
     * @return list of matching sessions
     */
    List<UserSession> findByUserAndIsLoggedOut(User user, Boolean isLoggedOut);

    /**
     * Finds a session by its JWT token string.
     *
     * <p>Derived query:
     * <pre>{@code SELECT * FROM user_sessions WHERE jwt_token = ?}</pre>
     *
     * <p>Used for token validation and logout flows.
     *
     * <p>Note: Field is {@code jwtToken} (maps to {@code jwt_token} column).
     * The previous {@code findByToken} was incorrect — the entity field
     * is named {@code jwtToken}, not {@code token}.
     *
     * @param jwtToken the JWT token string to search for
     * @return optional session matching the token
     */
    Optional<UserSession> findByJwtToken(String jwtToken);

    /**
     * Marks all sessions for a specific user as logged out.
     *
     * <p>Used for "logout from all devices" functionality.
     *
     * <p>JPQL references the entity field name {@code isLoggedOut},
     * not the DB column name {@code is_logged_out}.
     *
     * <p>Note: Previous query used {@code us.isActive = false} which is wrong —
     * the entity has no {@code isActive} field. Correct field is {@code isLoggedOut}.
     *
     * @param user user entity whose sessions should be invalidated
     */
    @Modifying
    @Query("UPDATE UserSession us SET us.isLoggedOut = true WHERE us.user = :user")
    void logoutAllSessionsByUser(User user);
}