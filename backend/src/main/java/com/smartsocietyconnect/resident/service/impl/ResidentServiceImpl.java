package com.smartsocietyconnect.resident.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.auth.repository.UserRepository;
import com.smartsocietyconnect.resident.dto.request.CreateResidentRequest;
import com.smartsocietyconnect.resident.dto.request.UpdateResidentRequest;
import com.smartsocietyconnect.resident.dto.response.ResidentResponse;
import com.smartsocietyconnect.resident.entity.Flat;
import com.smartsocietyconnect.resident.entity.FlatStatus;
import com.smartsocietyconnect.resident.entity.Resident;
import com.smartsocietyconnect.resident.exception.ResidentException;
import com.smartsocietyconnect.resident.repository.FlatRepository;
import com.smartsocietyconnect.resident.repository.ResidentRepository;
import com.smartsocietyconnect.resident.service.ResidentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the {@link ResidentService} interface providing all resident management operations.
 *
 * <p><strong>Core Responsibilities:</strong></p>
 * <ul>
 *   <li>CRUD operations for residents (Create, Read, Update, Delete)</li>
 *   <li>Linking users to flats (resident allocation)</li>
 *   <li>Managing primary member designation within a flat</li>
 *   <li>Automatic flat status synchronisation (OCCUPIED/VACANT) based on active residents</li>
 *   <li>Move-in and move-out workflows</li>
 * </ul>
 *
 * <p><strong>Business Rules Enforced:</strong></p>
 * <ul>
 *   <li>One user can have only one resident profile (enforced by {@code UNIQUE} constraint on {@code user_id})</li>
 *   <li>A flat can have multiple active residents (e.g., family members)</li>
 *   <li>Exactly one primary member per flat (the designated contact/head of family)</li>
 *   <li>Flats automatically become OCCUPIED when at least one active resident exists</li>
 *   <li>Flats automatically become VACANT when all residents have moved out</li>
 *   <li>Move-out date must be after move-in date</li>
 * </ul>
 *
 * <p><strong>Transaction Management:</strong></p>
 * <ul>
 *   <li>Class-level {@code @Transactional} ensures atomicity for all write operations</li>
 *   <li>Read operations are explicitly marked {@code readOnly = true} for performance optimisation</li>
 *   <li>Flat status updates are performed within the same transaction as resident updates</li>
 * </ul>
 *
 * <p><strong>Database Alignment (Design 3):</strong></p>
 * <ul>
 *   <li>{@code resident_id}: {@code INT UNSIGNED} (mapped to {@code Integer})</li>
 *   <li>{@code user_id}: {@code UNIQUE} to enforce one-to-one relationship</li>
 *   <li>{@code flat_id}: {@code SMALLINT UNSIGNED} (mapped to {@code Integer})</li>
 *   <li>{@code move_in_date}/{@code move_out_date}: {@code DATE}</li>
 *   <li>{@code is_primary_member}: {@code BOOLEAN}</li>
 * </ul>
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @since 2026-06-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ResidentServiceImpl implements ResidentService {

    private final ResidentRepository residentRepository;
    private final FlatRepository flatRepository;
    private final UserRepository userRepository;

    // ================================ CREATE ================================

    /**
     * Creates a new resident profile, linking an existing user to a flat.
     *
     * <p><strong>Business Logic:</strong></p>
     * <ul>
     *   <li>Validates that the user does not already have a resident profile</li>
     *   <li>Checks that the user is active (non-deactivated)</li>
     *   <li>Determines primary member status:
     *       <ul>
     *         <li>If the flat has no active residents, the new resident becomes the primary member</li>
     *         <li>If a primary member already exists, the new resident is a secondary member</li>
     *         <li>If the request explicitly sets {@code isPrimaryMember} to {@code true},
     *             the existing primary member is demoted (only one primary per flat)</li>
     *       </ul>
     *   </li>
     *   <li>Automatically updates the flat status to {@code OCCUPIED}</li>
     * </ul>
     *
     * <p><strong>Transactional Integrity:</strong> Both the resident and flat updates occur
     * within a single transaction, ensuring consistency.</p>
     *
     * @param request DTO containing user ID, flat ID, resident type, and optional primary member flag
     * @return {@link ResidentResponse} with the newly created resident details
     * @throws ResidentException if the user already has a profile, user is inactive,
     *                           or the flat/user doesn't exist
     */
    @Override
    public ResidentResponse createResident(CreateResidentRequest request) {
        log.info("Creating resident profile for user id: {}", request.getUserId());

        // Enforce one resident per user (database UNIQUE constraint also ensures this)
        if (residentRepository.existsByUserUserId(request.getUserId())) {
            throw new ResidentException("Resident profile already exists for user id: " + request.getUserId());
        }

        User user = findUserById(request.getUserId());
        Flat flat = findFlatById(request.getFlatId());

        // Only active users can be assigned as residents
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new ResidentException("Cannot create resident profile for inactive user id: " + request.getUserId());
        }

        // Fetch all currently active (non-moved-out) residents in the flat
        List<Resident> activeResidents = residentRepository
                .findByFlatFlatIdAndMoveOutDateIsNull(flat.getFlatId());

        // Resolve primary member status based on request and existing residents
        Boolean primaryMember = resolvePrimaryMember(request.getIsPrimaryMember(), activeResidents);

        // If this new resident is to be primary, demote any existing primary
        if (Boolean.TRUE.equals(primaryMember)) {
            clearPrimaryMembers(activeResidents);
        }

        // Build the resident entity
        Resident resident = Resident.builder()
                .user(user)
                .flat(flat)
                .residentType(request.getResidentType())
                .occupation(request.getOccupation())
                .emergencyContact(request.getEmergencyContact())
                .moveInDate(request.getMoveInDate())
                .isPrimaryMember(primaryMember)
                .build();

        // Mark flat as OCCUPIED since we now have at least one active resident
        flat.setStatus(FlatStatus.OCCUPIED);
        flatRepository.save(flat);

        Resident saved = residentRepository.save(resident);
        log.info("Resident profile created with id: {}", saved.getResidentId());

        return mapToResponse(saved);
    }

    // ================================ READ (SINGLE) ================================

    /**
     * Retrieves a resident profile by its ID.
     *
     * @param residentId the primary key of the resident
     * @return {@link ResidentResponse} containing full profile details
     * @throws ResidentException if the resident does not exist
     */
    @Override
    @Transactional(readOnly = true)
    public ResidentResponse getResidentById(Integer residentId) {
        log.debug("Fetching resident by id: {}", residentId);
        return mapToResponse(findResidentById(residentId));
    }

    /**
     * Retrieves the resident profile associated with a specific user.
     *
     * <p><strong>Use Case:</strong> When a user logs in, this method is called
     * to fetch their resident profile for display and profile management.</p>
     *
     * @param userId the ID of the user
     * @return {@link ResidentResponse} for the user
     * @throws ResidentException if the user is not associated with any resident profile
     */
    @Override
    @Transactional(readOnly = true)
    public ResidentResponse getResidentByUserId(Integer userId) {
        log.debug("Fetching resident by user id: {}", userId);
        Resident resident = residentRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResidentException(
                        "Resident not found for user id: " + userId));
        return mapToResponse(resident);
    }

    // ================================ READ (ALL) ================================

    /**
     * Retrieves all resident profiles in the system.
     *
     * <p><strong>Performance Note:</strong> For large societies, consider pagination
     * in a future iteration.</p>
     *
     * @return list of {@link ResidentResponse} for all residents
     */
    @Override
    @Transactional(readOnly = true)
    public List<ResidentResponse> getAllResidents() {
        log.debug("Fetching all residents");
        return residentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Retrieves all resident profiles associated with a specific flat.
     *
     * <p><strong>Use Case:</strong> Displaying the occupant list of a flat,
     * typically used by the admin or security module.</p>
     *
     * @param flatId the ID of the flat
     * @return list of residents living in the flat
     * @throws ResidentException if the flat does not exist
     */
    @Override
    @Transactional(readOnly = true)
    public List<ResidentResponse> getResidentsByFlat(Integer flatId) {
        log.debug("Fetching residents by flat id: {}", flatId);

        if (!flatRepository.existsById(flatId)) {
            throw new ResidentException("Flat not found with id: " + flatId);
        }

        return residentRepository.findByFlatFlatId(flatId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ================================ UPDATE ================================

    /**
     * Partially updates an existing resident profile.
     *
     * <p><strong>Business Rules:</strong></p>
     * <ul>
     *   <li>Only non-null fields in the request are updated (PATCH semantics)</li>
     *   <li>If {@code isPrimaryMember} is set to {@code true}, automatically demotes
     *       any existing primary member of the same flat</li>
     *   <li>If {@code moveOutDate} is provided, validates it is not before move-in date</li>
     *   <li>After update, the flat status is recalculated based on active residents</li>
     * </ul>
     *
     * @param residentId the ID of the resident to update
     * @param request DTO containing fields to update
     * @return updated {@link ResidentResponse}
     * @throws ResidentException if resident not found or move-out date is invalid
     */
    @Override
    public ResidentResponse updateResident(Integer residentId, UpdateResidentRequest request) {
        log.info("Updating resident id: {}", residentId);

        Resident resident = findResidentById(residentId);

        // Conditional updates - only non-null fields
        if (request.getResidentType() != null) {
            resident.setResidentType(request.getResidentType());
        }
        if (request.getOccupation() != null) {
            resident.setOccupation(request.getOccupation());
        }
        if (request.getEmergencyContact() != null) {
            resident.setEmergencyContact(request.getEmergencyContact());
        }
        if (request.getIsPrimaryMember() != null) {
            updatePrimaryMember(resident, request.getIsPrimaryMember());
        }
        if (request.getMoveOutDate() != null) {
            validateMoveOutDate(resident, request.getMoveOutDate());
            resident.setMoveOutDate(request.getMoveOutDate());
        }

        Resident updated = residentRepository.save(resident);

        // Recalculate flat status based on remaining active residents
        updateFlatStatusFromActiveResidents(updated.getFlat().getFlatId());

        log.info("Resident updated id: {}", updated.getResidentId());
        return mapToResponse(updated);
    }

    /**
     * Convenience method to move a resident out immediately (sets move-out date to today).
     *
     * <p><strong>Business Logic:</strong></p>
     * <ul>
     *   <li>Sets {@code moveOutDate} to {@link LocalDate#now()}</li>
     *   <li>Removes primary member status (since resident is no longer active)</li>
     *   <li>Automatically updates the flat status – if this was the last active resident,
     *       the flat becomes VACANT</li>
     * </ul>
     *
     * @param residentId the ID of the resident to move out
     * @return updated {@link ResidentResponse} with move-out date set
     * @throws ResidentException if resident is already moved out or not found
     */
    @Override
    public ResidentResponse moveOutResident(Integer residentId) {
        log.info("Moving out resident id: {}", residentId);

        Resident resident = findResidentById(residentId);

        if (resident.getMoveOutDate() != null) {
            throw new ResidentException("Resident already moved out with id: " + residentId);
        }

        resident.setMoveOutDate(LocalDate.now());
        resident.setIsPrimaryMember(false); // Inactive residents cannot be primary

        Resident updated = residentRepository.save(resident);

        // Recalculate flat status (may become VACANT if no active residents left)
        updateFlatStatusFromActiveResidents(updated.getFlat().getFlatId());

        return mapToResponse(updated);
    }

    // ================================ DELETE ================================

    /**
     * Permanently deletes a resident profile from the system.
     *
     * <p><strong>Important Business Rule:</strong></p>
     * <ul>
     *   <li>Deletion is allowed regardless of move-out status</li>
     *   <li>If the resident being deleted was the last active resident, the flat status
     *       is updated to {@code VACANT}</li>
     *   <li>For active residents, the flat remains OCCUPIED if other active residents exist</li>
     * </ul>
     *
     * <p><strong>Data Integrity:</strong></p>
     * <ul>
     *   <li>This is a hard delete; associated family_members will be cascaded only if
     *       {@code ON DELETE CASCADE} is configured in the schema (family_members table)</li>
     *   <li>No soft-delete mechanism is implemented for residents</li>
     * </ul>
     *
     * @param residentId the ID of the resident to delete
     * @throws ResidentException if the resident does not exist
     */
    @Override
    public void deleteResident(Integer residentId) {
        log.info("Deleting resident id: {}", residentId);

        Resident resident = findResidentById(residentId);
        Integer flatId = resident.getFlat().getFlatId();

        // Check if this resident is active (not moved out)
        boolean activeResident = resident.getMoveOutDate() == null;

        // If this is the only active resident in the flat, mark flat as VACANT
        if (activeResident && !hasOtherActiveResidents(flatId, residentId)) {
            Flat flat = resident.getFlat();
            flat.setStatus(FlatStatus.VACANT);
            flatRepository.save(flat);
        }

        residentRepository.delete(resident);
        log.info("Resident deleted id: {}", residentId);
    }

    // ================================ PRIVATE HELPERS ================================

    /**
     * Fetches a User entity by ID.
     *
     * @param userId the user ID
     * @return the User entity
     * @throws ResidentException if user not found
     */
    private User findUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResidentException(
                        "User not found with id: " + userId));
    }

    /**
     * Fetches a Flat entity by ID.
     *
     * @param flatId the flat ID
     * @return the Flat entity
     * @throws ResidentException if flat not found
     */
    private Flat findFlatById(Integer flatId) {
        return flatRepository.findById(flatId)
                .orElseThrow(() -> new ResidentException(
                        "Flat not found with id: " + flatId));
    }

    /**
     * Fetches a Resident entity by ID.
     *
     * @param residentId the resident ID
     * @return the Resident entity
     * @throws ResidentException if resident not found
     */
    private Resident findResidentById(Integer residentId) {
        return residentRepository.findById(residentId)
                .orElseThrow(() -> new ResidentException(
                        "Resident not found with id: " + residentId));
    }

    /**
     * Resolves the primary member status for a new resident.
     *
     * <p><strong>Logic:</strong></p>
     * <ul>
     *   <li>If the request explicitly specifies {@code isPrimaryMember}, use that</li>
     *   <li>Otherwise, the new resident becomes primary if the flat has no other active residents</li>
     * </ul>
     *
     * @param requestedPrimary the value from the request (may be null)
     * @param activeResidents list of current active residents in the flat
     * @return the resolved primary member status
     */
    private Boolean resolvePrimaryMember(Boolean requestedPrimary, List<Resident> activeResidents) {
        if (requestedPrimary != null) {
            return requestedPrimary;
        }
        // Default to primary only if no other active residents exist
        return activeResidents.isEmpty();
    }

    /**
     * Updates the primary member status for an existing resident.
     *
     * <p>If setting the resident to primary, demotes any existing primary member
     * of the same flat (ensuring exactly one primary per flat).</p>
     *
     * @param resident the resident to update
     * @param isPrimaryMember the new primary status
     */
    private void updatePrimaryMember(Resident resident, Boolean isPrimaryMember) {
        if (Boolean.TRUE.equals(isPrimaryMember)) {
            // Fetch all active residents in the same flat
            List<Resident> activeResidents = residentRepository
                    .findByFlatFlatIdAndMoveOutDateIsNull(resident.getFlat().getFlatId());

            // Demote any existing primary member, except the current resident
            clearPrimaryMembersExcept(activeResidents, resident.getResidentId());
        }

        resident.setIsPrimaryMember(isPrimaryMember);
    }

    /**
     * Demotes all residents in a list to non-primary.
     *
     * @param residents list of residents to clear primary flag for
     */
    private void clearPrimaryMembers(List<Resident> residents) {
        residents.forEach(resident -> resident.setIsPrimaryMember(false));
    }

    /**
     * Demotes all residents in a list to non-primary, except the one with the given ID.
     *
     * @param residents list of residents
     * @param residentId the resident to exclude from demotion
     */
    private void clearPrimaryMembersExcept(List<Resident> residents, Integer residentId) {
        residents.stream()
                .filter(resident -> !resident.getResidentId().equals(residentId))
                .forEach(resident -> resident.setIsPrimaryMember(false));
    }

    /**
     * Validates that the move-out date is not before the move-in date.
     *
     * @param resident the resident profile
     * @param moveOutDate the proposed move-out date
     * @throws ResidentException if move-out date is before move-in date
     */
    private void validateMoveOutDate(Resident resident, LocalDate moveOutDate) {
        if (moveOutDate.isBefore(resident.getMoveInDate())) {
            throw new ResidentException("Move-out date cannot be before move-in date");
        }
    }

    /**
     * Recalculates and updates the flat's occupancy status based on active residents.
     *
     * <p><strong>Business Rule:</strong> A flat is OCCUPIED if at least one resident
     * has not moved out; otherwise, it becomes VACANT.</p>
     *
     * @param flatId the ID of the flat to update
     */
    private void updateFlatStatusFromActiveResidents(Integer flatId) {
        Flat flat = findFlatById(flatId);

        boolean hasActiveResidents = !residentRepository
                .findByFlatFlatIdAndMoveOutDateIsNull(flatId)
                .isEmpty();

        flat.setStatus(hasActiveResidents ? FlatStatus.OCCUPIED : FlatStatus.VACANT);
        flatRepository.save(flat);
    }

    /**
     * Checks if a flat has any active residents other than the given resident ID.
     *
     * @param flatId the flat ID
     * @param residentId the resident ID to exclude
     * @return true if there is at least one other active resident
     */
    private boolean hasOtherActiveResidents(Integer flatId, Integer residentId) {
        return residentRepository.findByFlatFlatIdAndMoveOutDateIsNull(flatId)
                .stream()
                .anyMatch(resident -> !resident.getResidentId().equals(residentId));
    }

    /**
     * Maps a {@link Resident} entity to a {@link ResidentResponse} DTO.
     *
     * <p><strong>Purpose:</strong> Separates the internal entity structure from the API contract.
     * Includes user details (first name, last name, email, mobile) and flat details
     * (flat number, wing) to provide a rich response for the frontend.</p>
     *
     * @param resident the entity to map
     * @return the response DTO
     */
    private ResidentResponse mapToResponse(Resident resident) {
        User user = resident.getUser();
        Flat flat = resident.getFlat();

        return ResidentResponse.builder()
                .residentId(resident.getResidentId())
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .flatId(flat.getFlatId())
                .flatNumber(flat.getFlatNumber())
                .wing(flat.getWing())
                .residentType(resident.getResidentType())
                .occupation(resident.getOccupation())
                .emergencyContact(resident.getEmergencyContact())
                .moveInDate(resident.getMoveInDate())
                .moveOutDate(resident.getMoveOutDate())
                .isPrimaryMember(resident.getIsPrimaryMember())
                .createdAt(resident.getCreatedAt())
                .build();
    }
}