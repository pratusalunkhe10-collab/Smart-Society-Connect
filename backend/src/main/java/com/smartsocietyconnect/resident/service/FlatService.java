package com.smartsocietyconnect.resident.service;

import java.util.List;

import com.smartsocietyconnect.resident.dto.request.CreateFlatRequest;
import com.smartsocietyconnect.resident.dto.request.UpdateFlatRequest;
import com.smartsocietyconnect.resident.dto.response.FlatResponse;
import com.smartsocietyconnect.resident.entity.FlatStatus;
import com.smartsocietyconnect.resident.entity.FlatType;

/**
 * Service layer contract defining all business operations related to flat management.
 *
 * <p>This interface encapsulates the core flat CRUD operations, as well as search/filter
 * capabilities required by the Society Connect Resident Module.
 *
 * <p><strong>Database Alignment:</strong> All operations are designed to work with the
 * memory-optimized database schema (Design 3) where flats are stored with {@code SMALLINT}
 * IDs, {@code TINYINT} floors, and {@code ENUM} types for maximum performance under load.
 *
 * <p><strong>Business Rules:</strong>
 * <ul>
 *   <li>A flat cannot exist without a {@code flatNumber} (unique constraint enforced at DB level)</li>
 *   <li>Deleting a flat is permitted only if no residents are currently allocated to it</li>
 *   <li>Status changes (VACANT ↔ OCCUPIED) are typically triggered by resident move‑in/move‑out flows</li>
 * </ul>
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @since 2026-06-01
 */
public interface FlatService {

    /**
     * Creates a new flat record in the society database.
     *
     * <p><strong>Validation &amp; Business Logic:</strong></p>
     * <ul>
     *   <li>Validates that {@code flatNumber} does not already exist (throws {@code DuplicateResourceException})</li>
     *   <li>Defaults {@code status} to {@link FlatStatus#VACANT} if not explicitly provided</li>
     *   <li>Persists the flat with the memory-optimised types (SMALLINT for ID, TINYINT for floor)</li>
     * </ul>
     *
     * @param request DTO containing all required fields for new flat creation
     * @return {@link FlatResponse} containing the persisted flat details with generated ID
     * @throws com.smartsocietyconnect.common.exception.DuplicateResourceException if a flat with the same number already exists
     * @throws jakarta.validation.ValidationException if request validation fails
     */
    FlatResponse createFlat(CreateFlatRequest request);

    /**
     * Retrieves a specific flat by its unique numeric identifier.
     *
     * <p><strong>Use Case:</strong> Typically called when viewing flat details, editing a flat,
     * or fetching the flat associated with a resident profile.</p>
     *
     * @param flatId the primary key of the flat (maps to {@code SMALLINT UNSIGNED} in DB)
     * @return {@link FlatResponse} containing full flat details
     * @throws com.smartsocietyconnect.common.exception.ResourceNotFoundException if no flat exists with the given ID
     */
    FlatResponse getFlatById(Integer flatId);

    /**
     * Fetches all flats currently registered in the society.
     *
     * <p><strong>Performance Note:</strong> For large societies (e.g., 1000+ flats),
     * consider implementing pagination/sorting in future iterations to avoid memory overhead.</p>
     *
     * <p><strong>Ordering:</strong> Results are typically returned sorted by wing and floor number
     * for logical presentation in the UI.</p>
     *
     * @return list of {@link FlatResponse} objects representing all flats
     */
    List<FlatResponse> getAllFlats();

    /**
     * Retrieves all flats belonging to a specific building wing.
     *
     * <p><strong>Business Value:</strong> Used for wing-wise dashboards, maintenance scheduling,
     * or generating reports for specific blocks in the society.</p>
     *
     * @param wing the single uppercase letter designating the building block (e.g., "A", "B")
     * @return list of flats in the specified wing, ordered by floor number
     */
    List<FlatResponse> getFlatsByWing(String wing);

    /**
     * Fetches all flats filtered by their current occupancy status.
     *
     * <p><strong>Critical Use Case:</strong> Finding {@code VACANT} flats is essential for
     * the {@code ResidentService} during flat allocation when a new member registers.</p>
     *
     * @param status the occupancy status ({@link FlatStatus#VACANT} or {@link FlatStatus#OCCUPIED})
     * @return list of flats matching the given status
     */
    List<FlatResponse> getFlatsByStatus(FlatStatus status);

    /**
     * Retrieves all flats of a specific configuration type.
     *
     * <p><strong>Use Case:</strong> Allows residents to search for available flats by type
     * (e.g., 2BHK, 3BHK) during the registration flow, or for administrative reporting.</p>
     *
     * @param flatType the structural configuration (e.g., {@link FlatType#_2BHK}, {@link FlatType#_3BHK})
     * @return list of flats matching the requested type
     */
    List<FlatResponse> getFlatsByType(FlatType flatType);

    /**
     * Partially updates an existing flat's details.
     *
     * <p><strong>Important:</strong> Supports patch‑style updates – only the fields present
     * in the {@link UpdateFlatRequest} will be modified; null fields are ignored.</p>
     *
     * <p><strong>Critical Validation:</strong></p>
     * <ul>
     *   <li>If {@code flatNumber} is being changed, ensures the new number is not already taken</li>
     *   <li>Wing changes must be a valid single uppercase letter (enforced by validation)</li>
     *   <li>Area updates are constrained by the {@code SMALLINT} database limit (max 65,535 sqft)</li>
     * </ul>
     *
     * @param flatId  the ID of the flat to update
     * @param request DTO containing the fields to be updated
     * @return the updated {@link FlatResponse} reflecting the new state
     * @throws com.smartsocietyconnect.common.exception.ResourceNotFoundException if the flat does not exist
     */
    FlatResponse updateFlat(Integer flatId, UpdateFlatRequest request);

    /**
     * Dedicated method to change only the occupancy status of a flat.
     *
     * <p><strong>Business Trigger:</strong></p>
     * <ul>
     *   <li>Set to {@code OCCUPIED} when a resident successfully moves in and the allocation is finalized</li>
     *   <li>Set to {@code VACANT} when a resident moves out (move-out date set on the {@code residents} table)</li>
     * </ul>
     *
     * <p><strong>Transactional Note:</strong> This method is typically called within a broader transaction
     * that updates the corresponding {@code residents} record to maintain data consistency.</p>
     *
     * @param flatId the ID of the flat whose status is being changed
     * @param status the new status to apply
     * @return the updated {@link FlatResponse} with the new status
     * @throws com.smartsocietyconnect.common.exception.ResourceNotFoundException if the flat does not exist
     */
    FlatResponse updateFlatStatus(Integer flatId, FlatStatus status);

    /**
     * Permanently deletes a flat from the system.
     *
     * <p><strong>Strict Business Rule:</strong></p>
     * <ul>
     *   <li>Deletion is <strong>only allowed</strong> if the flat is currently {@code VACANT}
     *       and has no active residents linked to it</li>
     *   <li>If the flat has any associated records (residents, family_members, documents),
     *       the implementation must throw a {@code DataIntegrityViolationException}</li>
     *   <li>Soft-delete (via {@code is_deleted} flag) is <strong>not</strong> implemented for flats;
     *       hard deletion is used to keep the dataset lean and memory‑optimized</li>
     * </ul>
     *
     * <p><strong>Result:</strong> Removes the flat record entirely from the database, cascading
     * only if explicitly set to {@code ON DELETE CASCADE} in the schema (currently not recommended
     * for flats due to resident dependencies).</p>
     *
     * @param flatId the ID of the flat to be removed
     * @throws com.smartsocietyconnect.common.exception.ResourceNotFoundException if the flat does not exist
     * @throws org.springframework.dao.DataIntegrityViolationException if the flat is still occupied or linked to residents
     */
    void deleteFlat(Integer flatId);
}