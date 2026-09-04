package com.smartsocietyconnect.resident.dto.request;

import com.smartsocietyconnect.resident.entity.RelationType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for adding a family member to a resident profile
 * in Smart Society Connect.
 *
 * <p>Carries the input data from the client for the add-family-member endpoint.
 * The following fields are intentionally excluded:
 * <ul>
 *     <li>{@code memberId} — auto-generated primary key; never client-supplied</li>
 *     <li>{@code createdAt} — set automatically by Hibernate via
 *         {@code @CreationTimestamp}</li>
 * </ul>
 *
 * <p>All field-level constraints mirror the DB column definitions to ensure
 * validation failures are caught at the DTO layer and returned as
 * {@code 400 Bad Request}, rather than reaching the DB and causing
 * unhandled {@code DataIntegrityViolationException} (500 errors).
 *
 * <p><b>Security note on {@code residentId}:</b> The service layer must verify
 * that the authenticated user owns the resident profile identified by
 * {@code residentId} before adding a family member to it — a client
 * could otherwise supply a different resident's ID and add family
 * members to someone else's profile.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
@Getter
@Setter
public class AddFamilyMemberRequest {

    /**
     * ID of the resident profile this family member belongs to.
     *
     * <p>References {@code residents.resident_id}. The resident must exist
     * before a family member can be added to their profile.
     *
     * <p>See class-level security note regarding {@code residentId} trust.
     */
    @NotNull(message = "Resident ID is required")
    private Integer residentId;

    /**
     * Full name of the family member.
     *
     * <p>Maps to {@code member_name} column (VARCHAR(100), NOT NULL).
     *
     * <p>{@code @Size(max = 100)} mirrors the {@code VARCHAR(100)} DB constraint.
     * Without it, a value longer than 100 characters bypasses validation,
     * reaches the DB, and causes an unhandled {@code DataIntegrityViolationException}.
     */
    @NotBlank(message = "Member name is required")
    @Size(max = 100, message = "Member name must not exceed 100 characters")
    private String memberName;

    /**
     * Relationship of this family member to the primary resident.
     *
     * <p>Maps to {@code relation} column
     * ({@code ENUM('FATHER','MOTHER','SPOUSE','SON','DAUGHTER',
     * 'BROTHER','SISTER','OTHER')}, NOT NULL).
     *
     * <p>The service layer should use
     * {@code FamilyMemberRepository.existsByResidentResidentIdAndRelation()}
     * to enforce single-instance constraints for relations such as
     * {@link RelationType#SPOUSE}, {@link RelationType#FATHER}, and
     * {@link RelationType#MOTHER} before saving.
     */
    @NotNull(message = "Relation is required")
    private RelationType relation;

    /**
     * Age of the family member in years.
     *
     * <p>Maps to {@code age} column (TINYINT UNSIGNED, nullable).
     * Optional — may be omitted if not known.
     *
     * <p>{@code @Max(120)} applies a domain-level constraint (maximum
     * realistic human age) that is stricter than the DB's TINYINT UNSIGNED
     * limit of 255. This is intentional — values above 120 are implausible
     * and should be rejected before reaching the DB.
     *
     * <p>The service layer must convert this {@code Integer} value to
     * {@code Short} when building the
     * {@link com.smartsocietyconnect.resident.entity.FamilyMember} entity.
     */
    @Min(value = 0, message = "Age cannot be negative")
    @Max(value = 120, message = "Age must be 120 or below")
    private Byte  age;

    /**
     * Mobile number of the family member.
     *
     * <p>Maps to {@code mobile} column (CHAR(10), nullable).
     * Optional — must be exactly 10 digits if provided.
     *
     * <p>The service layer should call
     * {@code FamilyMemberRepository.existsByMobile()} before saving
     * to prevent the same mobile number being registered across
     * multiple family member records.
     */
    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "Mobile number must be exactly 10 digits"
    )
    private String mobile;

}