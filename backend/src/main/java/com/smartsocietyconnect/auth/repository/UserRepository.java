package com.smartsocietyconnect.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartsocietyconnect.auth.entity.User;

/**
 * Repository interface for {@link User} entity.
 *
 * <p>Provides CRUD and query operations for the {@code users} table.
 *
 * <p>Extends {@link JpaRepository} with generic types:
 * <ul>
 *     <li>{@link User}    — the entity type</li>
 *     <li>{@link Integer} — the primary key type, matching {@code user_id} (INT)</li>
 * </ul>
 *
 * <p>Spring Data JPA automatically provides standard operations including:
 * <ul>
 *     <li>{@code save(User)}          — insert or update</li>
 *     <li>{@code findById(Integer)}   — lookup by primary key</li>
 *     <li>{@code findAll()}           — retrieve all users</li>
 *     <li>{@code deleteById(Integer)} — remove by primary key</li>
 *     <li>{@code existsById(Integer)} — existence check by primary key</li>
 *     <li>{@code count()}             — total record count</li>
 * </ul>
 *
 * <p>Used heavily during:
 * <ul>
 *     <li>Login — load user by email or mobile for authentication</li>
 *     <li>Registration — check duplicate email / mobile before insert</li>
 *     <li>Email validation — resolve user from token or OTP flow</li>
 *     <li>Mobile validation — resolve user from OTP flow</li>
 * </ul>
 *
 * <p>Note: {@code @Repository} is technically redundant on {@link JpaRepository}
 * extensions, but is included explicitly as enterprise convention — it
 * documents intent clearly and enables Spring's persistence exception
 * translation layer.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see User
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // ==========================================================================
    // Lookup Methods
    // ==========================================================================

    /**
     * Finds a user by their email address.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM users WHERE email = ?}</pre>
     *
     * <p>Returns an {@link Optional} to force callers to handle
     * the case where no user with that email exists, avoiding
     * accidental {@code NullPointerException}.
     *
     * <p>Primarily used during login and password-reset flows.
     *
     * @param email the email address to search for (case-sensitive)
     * @return an {@link Optional} containing the matching {@link User},
     *         or {@link Optional#empty()} if no user with that email exists
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by their mobile number.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM users WHERE mobile = ?}</pre>
     *
     * <p>Returns an {@link Optional} to force callers to handle
     * the case where no user with that mobile exists, avoiding
     * accidental {@code NullPointerException}.
     *
     * <p>Primarily used during OTP-based login and mobile verification flows.
     *
     * @param mobile the 10-digit mobile number to search for
     * @return an {@link Optional} containing the matching {@link User},
     *         or {@link Optional#empty()} if no user with that mobile exists
     */
    Optional<User> findByMobile(String mobile);

    /**
     * Finds an active user by their email address.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM users WHERE email = ? AND is_active = ?}</pre>
     *
     * <p>Use this during authentication to prevent deactivated accounts
     * from logging in, without requiring a separate {@code isActive} check
     * in service layer code.
     *
     * @param email    the email address to search for (case-sensitive)
     * @param isActive pass {@code true} to fetch only active users
     * @return an {@link Optional} containing the matching active {@link User},
     *         or {@link Optional#empty()} if not found or account is inactive
     */
    Optional<User> findByEmailAndIsActive(String email, Boolean isActive);

    // ==========================================================================
    // Existence Check Methods
    // ==========================================================================

    /**
     * Checks whether a user with the given email already exists.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT COUNT(*) > 0 FROM users WHERE email = ?}</pre>
     *
     * <p><b>Prefer this over</b> {@code findByEmail(...).isPresent()} during
     * registration validation — this generates a lightweight {@code COUNT}
     * query and avoids loading the full {@link User} entity unnecessarily.
     *
     * @param email the email address to check (case-sensitive)
     * @return {@code true} if a user with this email exists, {@code false} otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Checks whether a user with the given mobile number already exists.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT COUNT(*) > 0 FROM users WHERE mobile = ?}</pre>
     *
     * <p><b>Prefer this over</b> {@code findByMobile(...).isPresent()} during
     * registration validation — this generates a lightweight {@code COUNT}
     * query and avoids loading the full {@link User} entity unnecessarily.
     *
     * @param mobile the 10-digit mobile number to check
     * @return {@code true} if a user with this mobile exists, {@code false} otherwise
     */
    boolean existsByMobile(String mobile);

}