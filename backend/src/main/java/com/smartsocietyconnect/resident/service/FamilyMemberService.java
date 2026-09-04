package com.smartsocietyconnect.resident.service;

import java.util.List;

import com.smartsocietyconnect.resident.dto.request.AddFamilyMemberRequest;
import com.smartsocietyconnect.resident.dto.request.UpdateFamilyMemberRequest;
import com.smartsocietyconnect.resident.dto.response.FamilyMemberResponse;

/**
 * Service interface for family member operations in Smart Society Connect.
 *
 * <p>Defines the business logic contract for managing family members
 * linked to resident profiles. Implementations handle:
 * <ul>
 *     <li>Creating, reading, updating, and deleting family member records</li>
 *     <li>Validating single-instance relation constraints (SPOUSE, FATHER, MOTHER)</li>
 *     <li>Checking mobile number uniqueness across all family member records</li>
 *     <li>Enforcing family size limits per resident</li>
 * </ul>
 *
 * <p>The standard implementation is
 * {@code com.smartsocietyconnect.resident.service.impl.FamilyMemberServiceImpl}.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see com.smartsocietyconnect.resident.entity.FamilyMember
 * @see com.smartsocietyconnect.resident.repository.FamilyMemberRepository
 */
public interface FamilyMemberService {

    // ==========================================================================
    // Create
    // ==========================================================================

    /**
     * Adds a new family member to a resident's profile.
     *
     * <p>The implementation must:
     * <ol>
     *     <li>Verify the resident exists</li>
     *     <li>Enforce single-instance relation constraints — reject if the resident
     *         already has a {@code SPOUSE}, {@code FATHER}, or {@code MOTHER}
     *         registered (use {@code existsByResidentResidentIdAndRelation})</li>
     *     <li>Validate mobile uniqueness if provided
     *         (use {@code existsByMobile})</li>
     *     <li>Save and return the created record as a {@link FamilyMemberResponse}</li>
     * </ol>
     *
     * @param request the validated request DTO containing family member details
     * @return the created {@link FamilyMemberResponse} including the generated
     *         {@code memberId} and {@code createdAt} timestamp
     * @throws com.smartsocietyconnect.auth.exception.AuthException
     *         if the resident does not exist, the relation constraint is violated,
     *         or the mobile number is already registered
     */
    FamilyMemberResponse addFamilyMember(AddFamilyMemberRequest request);

    // ==========================================================================
    // Read
    // ==========================================================================

    /**
     * Retrieves a single family member record by its primary key.
     *
     * @param memberId the {@code member_id} of the family member to retrieve
     * @return the matching {@link FamilyMemberResponse}
     * @throws com.smartsocietyconnect.auth.exception.AuthException
     *         if no family member with the given ID exists
     */
    FamilyMemberResponse getFamilyMemberById(Integer memberId);

    /**
     * Retrieves all family members linked to a specific resident.
     *
     * @param residentId the {@code resident_id} of the parent resident
     * @return a {@link List} of {@link FamilyMemberResponse}s for this resident;
     *         never {@code null} — returns an empty list if the resident has no
     *         registered family members
     * @throws com.smartsocietyconnect.auth.exception.AuthException
     *         if no resident with the given ID exists
     */
    List<FamilyMemberResponse> getFamilyMembersByResident(Integer residentId);

    /**
     * Returns the total number of family members registered under a resident.
     *
     * <p>Uses a lightweight {@code COUNT} query via
     * {@code FamilyMemberRepository.countByResidentResidentId()} — does not
     * load any {@link com.smartsocietyconnect.resident.entity.FamilyMember}
     * entities into memory.
     *
     * <p>Use this to enforce a maximum family size limit before calling
     * {@link #addFamilyMember(AddFamilyMemberRequest)}.
     *
     * @param residentId the {@code resident_id} of the parent resident
     * @return the number of family members; {@code 0} if none registered
     */
    long countFamilyMembersByResident(Integer residentId);

    // ==========================================================================
    // Update
    // ==========================================================================

    /**
     * Updates an existing family member's mutable details.
     *
     * <p>Only mutable fields should be accepted in {@link UpdateFamilyMemberRequest}
     * — {@code residentId} and {@code relation} should NOT be updatable after
     * creation, as changing them would alter the semantic identity of the record.
     *
     * <p>The implementation must:
     * <ol>
     *     <li>Verify the family member exists</li>
     *     <li>If mobile is being updated, validate it is not already registered
     *         to a different family member (use {@code existsByMobile})</li>
     *     <li>Apply changes and return the updated record</li>
     * </ol>
     *
     * @param memberId the {@code member_id} of the family member to update
     * @param request  the validated request DTO containing updated field values
     * @return the updated {@link FamilyMemberResponse}
     * @throws com.smartsocietyconnect.auth.exception.AuthException
     *         if no family member with the given ID exists, or if the updated
     *         mobile number is already registered to another family member
     */
    FamilyMemberResponse updateFamilyMember(
            Integer memberId,
            UpdateFamilyMemberRequest request
    );

    // ==========================================================================
    // Delete
    // ==========================================================================

    /**
     * Deletes a family member record by its primary key.
     *
     * <p>Only removes the individual {@link com.smartsocietyconnect.resident.entity.FamilyMember}
     * record — does not affect the parent
     * {@link com.smartsocietyconnect.resident.entity.Resident} or any other records.
     *
     * @param memberId the {@code member_id} of the family member to delete
     * @throws com.smartsocietyconnect.auth.exception.AuthException
     *         if no family member with the given ID exists
     */
    void deleteFamilyMember(Integer memberId);

}