package com.smartsocietyconnect.auth.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartsocietyconnect.auth.entity.Role;
import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.auth.entity.UserRole;
import com.smartsocietyconnect.auth.entity.UserRoleId;

/**
 * Repository interface for {@link UserRole} entity.
 *
 * <p>Provides CRUD and query operations for the {@code user_roles} join table,
 * which maps the many-to-many relationship between {@link User} and {@link Role}.
 *
 * <p>Extends {@link JpaRepository} with generic types:
 * <ul>
 *     <li>{@link UserRole}   — the entity type</li>
 *     <li>{@link UserRoleId} — the composite primary key type ({@code user_id + role_id})</li>
 * </ul>
 *
 * <p>Spring Data JPA automatically provides standard operations including:
 * <ul>
 *     <li>{@code save(UserRole)}            — insert a new user-role mapping</li>
 *     <li>{@code findById(UserRoleId)}      — lookup by composite key</li>
 *     <li>{@code existsById(UserRoleId)}    — check mapping existence by composite key</li>
 *     <li>{@code deleteById(UserRoleId)}    — remove a specific user-role mapping</li>
 *     <li>{@code findAll()}                 — retrieve all mappings</li>
 *     <li>{@code count()}                   — total mapping count</li>
 * </ul>
 *
 * <p>Used during:
 * <ul>
 *     <li>Registration — assign default role to new user</li>
 *     <li>Authorization — load all roles for a user at login</li>
 *     <li>Admin — assign or revoke roles from users</li>
 *     <li>User deletion — remove all role mappings before deleting user</li>
 * </ul>
 *
 * <p>Note: {@code @Repository} is technically redundant on {@link JpaRepository}
 * extensions, but is included explicitly as enterprise convention — it
 * documents intent clearly and enables Spring's persistence exception
 * translation layer.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see UserRole
 * @see UserRoleId
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    // ==========================================================================
    // Lookup Methods
    // ==========================================================================

    /**
     * Finds all role mappings for a specific user.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM user_roles WHERE user_id = ?}</pre>
     *
     * <p>Primarily used during login to load all roles assigned to a user
     * for building the Spring Security {@code GrantedAuthority} list.
     *
     * @param user the {@link User} entity whose role mappings are to be retrieved
     * @return a {@link List} of {@link UserRole} mappings for the given user;
     *         never {@code null} — returns an empty list if the user has no roles
     */
    List<UserRole> findByUser(User user);

    /**
     * Finds all user mappings for a specific role.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM user_roles WHERE role_id = ?}</pre>
     *
     * <p>Primarily used by admin features to list all users
     * assigned to a given role (e.g. "list all ADMIN users").
     *
     * @param role the {@link Role} entity whose user mappings are to be retrieved
     * @return a {@link List} of {@link UserRole} mappings for the given role;
     *         never {@code null} — returns an empty list if no users have this role
     */
    List<UserRole> findByRole(Role role);

    // ==========================================================================
    // Existence Check Methods
    // ==========================================================================

    /**
     * Checks whether a specific user already has a specific role assigned.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT COUNT(*) > 0 FROM user_roles WHERE user_id = ? AND role_id = ?}</pre>
     *
     * <p><b>Always call this before {@code save()} during role assignment</b>
     * to prevent a duplicate key violation on the composite primary key
     * ({@code user_id + role_id}).
     *
     * <p><b>Prefer this over</b> {@code findByUser(...)} + manual stream filtering —
     * this generates a lightweight {@code COUNT} query without loading
     * any {@link UserRole} entities.
     *
     * @param user the {@link User} to check
     * @param role the {@link Role} to check
     * @return {@code true} if the user already has this role assigned,
     *         {@code false} otherwise
     */
    boolean existsByUserAndRole(User user, Role role);

    // ==========================================================================
    // Delete Methods
    // ==========================================================================

    /**
     * Deletes all role mappings for a specific user.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code DELETE FROM user_roles WHERE user_id = ?}</pre>
     *
     * <p>Must be called before deleting a {@link User} entity to avoid
     * foreign key constraint violations. Also used when resetting a
     * user's roles entirely before reassigning new ones.
     *
     * @param user the {@link User} entity whose role mappings are to be removed
     */
    void deleteByUser(User user);

    /** Removes one specific user-role mapping without affecting other roles. */
    void deleteByUserAndRole(User user, Role role);

}
