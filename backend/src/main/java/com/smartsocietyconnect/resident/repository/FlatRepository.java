package com.smartsocietyconnect.resident.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartsocietyconnect.resident.entity.Flat;
import com.smartsocietyconnect.resident.entity.FlatStatus;
import com.smartsocietyconnect.resident.entity.FlatType;

/**
 * Repository interface for {@link Flat} entity.
 *
 * <p>Provides CRUD and query operations for the {@code flats} table.
 *
 * <p>Extends {@link JpaRepository} with generic types:
 * <ul>
 *     <li>{@link Flat}    — the entity type</li>
 *     <li>{@link Integer} — the primary key type, matching {@code flat_id} (SMALLINT UNSIGNED)</li>
 * </ul>
 *
 * <p>Spring Data JPA automatically provides standard operations including:
 * <ul>
 *     <li>{@code save(Flat)}          — insert or update a flat record</li>
 *     <li>{@code findById(Integer)}   — lookup by primary key</li>
 *     <li>{@code findAll()}           — retrieve all flats</li>
 *     <li>{@code deleteById(Integer)} — remove by primary key</li>
 *     <li>{@code count()}             — total flat count</li>
 * </ul>
 *
 * <p>Used during:
 * <ul>
 *     <li>Flat registration — check duplicate flat number before insert</li>
 *     <li>Resident assignment — find vacant flats available for occupancy</li>
 *     <li>Admin dashboard — list and filter flats by wing, type, and status</li>
 * </ul>
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see Flat
 */
@Repository
public interface FlatRepository extends JpaRepository<Flat, Integer> {

    // ==========================================================================
    // Lookup Methods
    // ==========================================================================

    /**
     * Finds a flat by its unique flat number.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM flats WHERE flat_number = ?}</pre>
     *
     * <p>Primarily used when looking up a specific flat during
     * resident assignment or flat detail retrieval.
     *
     * @param flatNumber the unique flat identifier (e.g. {@code "A101"}, {@code "B203"})
     * @return an {@link Optional} containing the matching {@link Flat},
     *         or {@link Optional#empty()} if no flat with that number exists
     */
    Optional<Flat> findByFlatNumber(String flatNumber);

    /**
     * Finds all flats with a given occupancy status.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM flats WHERE status = ?}</pre>
     *
     * <p><b>This is the primary method for resident assignment flows.</b>
     * Pass {@link FlatStatus#VACANT} to retrieve all flats available
     * for a new resident. Without this method, the only alternative is
     * {@code findAll()} + Java stream filter — which loads every flat
     * in the society into memory before filtering.
     *
     * @param status the occupancy status to filter by
     *               ({@link FlatStatus#VACANT} or {@link FlatStatus#OCCUPIED})
     * @return a {@link List} of {@link Flat}s with the given status;
     *         never {@code null} — returns an empty list if none found
     */
    List<Flat> findByStatus(FlatStatus status);

    /**
     * Finds all flats belonging to a specific wing.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM flats WHERE wing = ?}</pre>
     *
     * <p>Used in admin views to list all flats in a given building wing
     * (e.g. "Wing A", "Wing B") for display or reporting.
     *
     * @param wing the single-character wing identifier (e.g. {@code "A"}, {@code "B"})
     * @return a {@link List} of {@link Flat}s in the specified wing;
     *         never {@code null} — returns an empty list if the wing has no flats
     */
    List<Flat> findByWing(String wing);

    /**
     * Finds all flats of a given configuration type.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM flats WHERE flat_type = ?}</pre>
     *
     * <p>Used to filter flats by size/configuration, for example to show
     * all available 2BHK flats or generate a count breakdown by type.
     *
     * @param flatType the flat configuration to filter by (e.g. {@link FlatType#_2BHK})
     * @return a {@link List} of {@link Flat}s of the specified type;
     *         never {@code null} — returns an empty list if none found
     */
    List<Flat> findByFlatType(FlatType flatType);

    /**
     * Finds all flats with a given status within a specific wing.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM flats WHERE status = ? AND wing = ?}</pre>
     *
     * <p>Used during resident assignment to show only the vacant flats
     * available within a particular wing — a common admin workflow when
     * a new resident requests a specific block of the society.
     *
     * @param status the occupancy status to filter by
     * @param wing   the single-character wing identifier (e.g. {@code "A"})
     * @return a {@link List} of {@link Flat}s matching both status and wing;
     *         never {@code null} — returns an empty list if none found
     */
    List<Flat> findByStatusAndWing(FlatStatus status, String wing);

    // ==========================================================================
    // Existence Check Methods
    // ==========================================================================

    /**
     * Checks whether a flat with the given flat number already exists.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT COUNT(*) > 0 FROM flats WHERE flat_number = ?}</pre>
     *
     * <p><b>Prefer this over</b> {@code findByFlatNumber(...).isPresent()} during
     * flat registration validation — this generates a lightweight {@code COUNT}
     * query and avoids loading the full {@link Flat} entity unnecessarily.
     *
     * @param flatNumber the flat identifier to check (e.g. {@code "A101"})
     * @return {@code true} if a flat with this number already exists,
     *         {@code false} otherwise
     */
    boolean existsByFlatNumber(String flatNumber);
    
	 // ==========================================================================
	 // Dashboard Statistics
	 // ==========================================================================
	
	 long countByStatus(FlatStatus status);
	
	 long countByWing(String wing);
	
	 long countByFlatType(FlatType flatType);

}