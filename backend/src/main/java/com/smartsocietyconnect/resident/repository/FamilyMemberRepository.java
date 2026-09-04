package com.smartsocietyconnect.resident.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartsocietyconnect.resident.entity.FamilyMember;
import com.smartsocietyconnect.resident.entity.RelationType;

/**
 * Repository interface for {@link FamilyMember} entity.
 *
 * <p>Provides CRUD and query operations for the {@code family_members} table.
 *
 * <p>Extends {@link JpaRepository} with generic types:
 * <ul>
 *     <li>{@link FamilyMember} — the entity type</li>
 *     <li>{@link Integer}      — the primary key type, matching {@code member_id} (INT UNSIGNED)</li>
 * </ul>
 *
 * <p>Spring Data JPA automatically provides standard operations including:
 * <ul>
 *     <li>{@code save(FamilyMember)}    — insert or update a family member record</li>
 *     <li>{@code findById(Integer)}     — lookup by primary key</li>
 *     <li>{@code deleteById(Integer)}   — remove a single family member by primary key</li>
 *     <li>{@code count()}               — total family member count across all residents</li>
 * </ul>
 *
 * <p><b>Cascade deletion note:</b> The {@code family_members} table declares
 * {@code ON DELETE CASCADE} on the {@code resident_id} FK. Deleting a
 * {@link com.smartsocietyconnect.resident.entity.Resident} automatically removes
 * all associated {@link FamilyMember} rows at the database level. No
 * {@code deleteByResidentResidentId} method is needed — adding one risks a
 * double-delete conflict with the DB cascade.
 *
 * <p>Used during:
 * <ul>
 *     <li>Resident registration — add family members to a resident profile</li>
 *     <li>Profile management — list, update, or remove family members</li>
 *     <li>Validation — check for duplicate mobile numbers or relation constraints</li>
 * </ul>
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see FamilyMember
 */
@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Integer> {

    // ==========================================================================
    // Lookup Methods
    // ==========================================================================

    /**
     * Finds all family members registered under a specific resident.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM family_members WHERE resident_id = ?}</pre>
     *
     * <p>Used to display the full family composition on a resident's profile page.
     *
     * @param residentId the {@code resident_id} of the parent
     *                   {@link com.smartsocietyconnect.resident.entity.Resident}
     * @return a {@link List} of {@link FamilyMember}s belonging to the given resident;
     *         never {@code null} — returns an empty list if the resident has no
     *         registered family members
     */
    List<FamilyMember> findByResidentResidentId(Integer residentId);

    /**
     * Finds all family members of a specific relation type under a resident.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM family_members WHERE resident_id = ? AND relation = ?}</pre>
     *
     * <p>Useful for validating uniqueness constraints before registration
     * (e.g. a resident should have at most one {@link RelationType#SPOUSE},
     * one {@link RelationType#FATHER}, one {@link RelationType#MOTHER}).
     * Also used to display or filter family members by category.
     *
     * @param residentId the {@code resident_id} of the parent resident
     * @param relation   the {@link RelationType} to filter by
     * @return a {@link List} of {@link FamilyMember}s of the specified relation;
     *         never {@code null} — returns an empty list if none found
     */
    List<FamilyMember> findByResidentResidentIdAndRelation(
            Integer residentId,
            RelationType relation
    );

    // ==========================================================================
    // Count Methods
    // ==========================================================================

    /**
     * Counts the total number of family members registered under a resident.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT COUNT(*) FROM family_members WHERE resident_id = ?}</pre>
     *
     * <p>Useful for enforcing a maximum family size limit per resident
     * without loading the full list of {@link FamilyMember} entities.
     *
     * @param residentId the {@code resident_id} of the parent resident
     * @return the count of family members; {@code 0} if none registered
     */
    long countByResidentResidentId(Integer residentId);

    // ==========================================================================
    // Existence Check Methods
    // ==========================================================================

    /**
     * Checks whether a given mobile number is already registered
     * to any family member across the society.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT COUNT(*) > 0 FROM family_members WHERE mobile = ?}</pre>
     *
     * <p>Used during family member registration to prevent duplicate mobile
     * numbers across all family member records.
     *
     * @param mobile the 10-digit mobile number to check
     * @return {@code true} if the mobile is already registered to a family member,
     *         {@code false} otherwise
     */
    boolean existsByMobile(String mobile);

    /**
     * Checks whether a resident already has a family member of a specific
     * relation type registered.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT COUNT(*) > 0 FROM family_members
     *WHERE resident_id = ? AND relation = ?}</pre>
     *
     * <p><b>Use this before saving</b> to enforce single-instance relations.
     * For example, a resident can logically have only one
     * {@link RelationType#SPOUSE}, one {@link RelationType#FATHER}, and one
     * {@link RelationType#MOTHER}. Without this check, the same relation
     * can be registered multiple times without any DB-level constraint stopping it.
     *
     * <p><b>Prefer this over</b>
     * {@code findByResidentResidentIdAndRelation(...).isEmpty()} when
     * only a boolean answer is needed — avoids loading full entities.
     *
     * @param residentId the {@code resident_id} of the parent resident
     * @param relation   the {@link RelationType} to check
     * @return {@code true} if the resident already has a family member
     *         with this relation, {@code false} otherwise
     */
    boolean existsByResidentResidentIdAndRelation(
            Integer residentId,
            RelationType relation
    );
}