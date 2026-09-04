package com.smartsocietyconnect.resident.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartsocietyconnect.resident.entity.Resident;
import com.smartsocietyconnect.resident.entity.ResidentType;

/**
 * Repository interface for {@link Resident} entity.
 *
 * <p>Provides CRUD and query operations for the {@code residents} table.
 *
 * <p>Extends {@link JpaRepository} with generic types:
 * <ul>
 *     <li>{@link Resident} — the entity type</li>
 *     <li>{@link Integer}  — the primary key type, matching {@code resident_id} (INT UNSIGNED)</li>
 * </ul>
 *
 * <p>Spring Data JPA automatically provides standard operations including:
 * <ul>
 *     <li>{@code save(Resident)}          — insert or update a resident record</li>
 *     <li>{@code findById(Integer)}       — lookup by primary key</li>
 *     <li>{@code deleteById(Integer)}     — remove by primary key</li>
 *     <li>{@code count()}                 — total resident count</li>
 * </ul>
 *
 * <p><b>Active vs historical residents:</b> The {@code move_out_date} column
 * is {@code null} for currently active residents and set to a date when they
 * leave. All queries that need <em>current</em> occupants must include
 * {@code AndMoveOutDateIsNull} — omitting this returns historical records too.
 *
 * <p>Used during:
 * <ul>
 *     <li>Registration — check if user already has a resident profile</li>
 *     <li>Login / profile — load resident profile linked to a user account</li>
 *     <li>Flat management — list current and historical residents of a flat</li>
 *     <li>Admin dashboard — filter residents by type or active status</li>
 * </ul>
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see Resident
 */
@Repository
public interface ResidentRepository extends JpaRepository<Resident, Integer> {

    // ==========================================================================
    // User-based Lookup
    // ==========================================================================

    /**
     * Finds the resident profile linked to a specific user account.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM residents WHERE user_id = ?}</pre>
     *
     * <p>Used after login to load the authenticated user's resident profile
     * and flat details. The {@code user_id} column has a UNIQUE constraint,
     * so at most one record will be returned.
     *
     * @param userId the {@code user_id} of the associated {@link com.smartsocietyconnect.auth.entity.User}
     * @return an {@link Optional} containing the matching {@link Resident},
     *         or {@link Optional#empty()} if no resident profile exists for this user
     */
    Optional<Resident> findByUserUserId(Integer userId);

    // ==========================================================================
    // Flat-based Lookup
    // ==========================================================================

    /**
     * Finds all resident records — active and historical — linked to a flat.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM residents WHERE flat_id = ?}</pre>
     *
     * <p><b>Includes ex-residents</b> whose {@code move_out_date} is set.
     * Use this for auditing and historical reporting.
     * For <em>current</em> occupants only, use
     * {@link #findByFlatFlatIdAndMoveOutDateIsNull(Integer)} instead.
     *
     * @param flatId the {@code flat_id} of the target {@link com.smartsocietyconnect.resident.entity.Flat}
     * @return a {@link List} of all {@link Resident} records ever linked to this flat;
     *         never {@code null} — returns an empty list if no residents found
     */
    List<Resident> findByFlatFlatId(Integer flatId);

    /**
     * Finds all <em>currently active</em> residents of a specific flat.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM residents WHERE flat_id = ? AND move_out_date IS NULL}</pre>
     *
     * <p><b>This is the primary method for flat occupancy queries.</b>
     * A {@code null} {@code move_out_date} indicates the resident is still
     * living in the flat. Ex-residents with a set {@code move_out_date}
     * are excluded.
     *
     * <p>Use this to determine current occupants before:
     * <ul>
     *     <li>Marking a flat as {@code OCCUPIED} or {@code VACANT}</li>
     *     <li>Sending society notices to flat residents</li>
     *     <li>Displaying current occupancy on the admin dashboard</li>
     * </ul>
     *
     * @param flatId the {@code flat_id} of the target flat
     * @return a {@link List} of currently active {@link Resident}s in this flat;
     *         never {@code null} — returns an empty list if the flat is vacant
     */
    List<Resident> findByFlatFlatIdAndMoveOutDateIsNull(Integer flatId);

    // ==========================================================================
    // Type and Status Filters
    // ==========================================================================

    /**
     * Finds all residents of a specific type (owner or tenant) across the society.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM residents WHERE resident_type = ?}</pre>
     *
     * <p>Used in admin reports to list all owners or all tenants
     * registered in the society.
     *
     * @param residentType the residency type to filter by
     *                     ({@link ResidentType#OWNER} or {@link ResidentType#TENANT})
     * @return a {@link List} of {@link Resident}s of the given type;
     *         never {@code null} — returns an empty list if none found
     */
    List<Resident> findByResidentType(ResidentType residentType);

    /**
     * Finds all currently active residents across the entire society.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM residents WHERE move_out_date IS NULL}</pre>
     *
     * <p>A {@code null} {@code move_out_date} indicates the resident is
     * currently living in their assigned flat.
     *
     * <p>Used for society-wide operations such as broadcasting notices,
     * generating occupancy reports, or maintenance scheduling.
     *
     * @return a {@link List} of all currently active {@link Resident}s;
     *         never {@code null} — returns an empty list if none found
     */
    List<Resident> findByMoveOutDateIsNull();

    /**
     * Finds all primary members across the society.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM residents WHERE is_primary_member = 1}</pre>
     *
     * <p>Each flat should have exactly one primary member — typically the
     * owner or the primary tenant. Used for society communication where
     * only one contact per flat is required.
     *
     * @return a {@link List} of {@link Resident}s flagged as primary members;
     *         never {@code null} — returns an empty list if none found
     */
    List<Resident> findByIsPrimaryMemberTrue();

    // ==========================================================================
    // Existence Check Methods
    // ==========================================================================

    /**
     * Checks whether a resident profile already exists for the given user.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT COUNT(*) > 0 FROM residents WHERE user_id = ?}</pre>
     *
     * <p><b>Prefer this over</b> {@code findByUserUserId(...).isPresent()} during
     * registration — this generates a lightweight {@code COUNT} query and avoids
     * loading the full {@link Resident} entity unnecessarily.
     *
     * @param userId the {@code user_id} to check
     * @return {@code true} if a resident profile exists for this user,
     *         {@code false} otherwise
     */
    boolean existsByUserUserId(Integer userId);

    /**
     * Checks whether a flat has any resident records (active or historical).
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT COUNT(*) > 0 FROM residents WHERE flat_id = ?}</pre>
     *
     * <p><b>Prefer this over</b> {@code findByFlatFlatId(...).isEmpty()} when
     * only a boolean answer is needed — avoids loading full entity list.
     *
     * <p>To check only <em>current</em> occupancy (excluding ex-residents),
     * use {@code !findByFlatFlatIdAndMoveOutDateIsNull(flatId).isEmpty()} or
     * add a dedicated {@code existsByFlatFlatIdAndMoveOutDateIsNull} method
     * if query volume justifies it.
     *
     * @param flatId the {@code flat_id} to check
     * @return {@code true} if any resident (current or historical) is linked
     *         to this flat, {@code false} if the flat has never had a resident
     */
    boolean existsByFlatFlatId(Integer flatId);
    
	 // ==========================================================================
	 // Dashboard Statistics
	 // ==========================================================================
	
	 long countByMoveOutDateIsNull();
	
	 long countByMoveOutDateIsNotNull();
	
	 long countByResidentType(ResidentType residentType);
	
	 long countByIsPrimaryMemberTrue();

}