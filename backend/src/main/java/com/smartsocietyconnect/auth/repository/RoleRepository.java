package com.smartsocietyconnect.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartsocietyconnect.auth.entity.Role;

/**
 * Repository interface for {@link Role} entity.
 *
 * <p>Provides CRUD and query operations for the {@code roles} table.
 *
 * <p>Extends {@link JpaRepository} with generic types:
 * <ul>
 *     <li>{@link Role}  — the entity type</li>
 *     <li>{@link Short} — the primary key type, matching {@code role_id} (TINYINT)</li>
 * </ul>
 *
 * <p>Spring Data JPA automatically provides standard operations including:
 * <ul>
 *     <li>{@code save(Role)}         — insert or update</li>
 *     <li>{@code findById(Short)}    — lookup by primary key</li>
 *     <li>{@code findAll()}          — retrieve all roles</li>
 *     <li>{@code deleteById(Short)}  — remove by primary key</li>
 *     <li>{@code existsById(Short)}  — existence check</li>
 *     <li>{@code count()}            — total record count</li>
 * </ul>
 *
 * <p>Note: {@code @Repository} is technically redundant on {@link JpaRepository}
 * extensions, but is included explicitly as enterprise convention — it
 * documents intent clearly and enables Spring's persistence exception
 * translation layer.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see Role
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Byte > {

    /**
     * Finds a role by its unique name.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM roles WHERE role_name = ?}</pre>
     *
     * <p>Returns an {@link Optional} to force callers to handle
     * the case where the role does not exist, avoiding accidental
     * {@code NullPointerException}.
     *
     * @param roleName the exact role name to search for (case-sensitive)
     * @return an {@link Optional} containing the matching {@link Role},
     *         or {@link Optional#empty()} if no role with that name exists
     */
    Optional<Role> findByRoleName(String roleName);

}
