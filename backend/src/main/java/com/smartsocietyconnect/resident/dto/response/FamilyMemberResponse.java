package com.smartsocietyconnect.resident.dto.response;

import java.time.LocalDateTime;

import com.smartsocietyconnect.resident.entity.RelationType;

import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO returned after family member-related operations.
 *
 * <p>Represents a read-only snapshot of a {@code family_members}
 * table record, returned to the client after create, update,
 * or fetch operations.
 *
 * <p>This is an outbound-only DTO — never deserialized from client
 * requests. {@code @Builder} is sufficient; no {@code @NoArgsConstructor}
 * or {@code @AllArgsConstructor} is needed since Jackson only
 * serializes (writes) this object to JSON.
 *
 * <p>Field types match the {@code FamilyMember} entity exactly
 * to avoid silent type conversion errors during mapping.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see com.smartsocietyconnect.resident.entity.FamilyMember
 * @see RelationType
 */
@Getter
@Builder
public class FamilyMemberResponse {

    /**
     * Unique identifier of the family member record.
     *
     * <p>Maps from {@code family_members.member_id}
     * (INT UNSIGNED → Integer).
     */
    private Integer memberId;

    /**
     * ID of the resident this family member belongs to.
     *
     * <p>Maps from {@code family_members.resident_id}
     * (INT UNSIGNED → Integer).
     * References {@code residents.resident_id}.
     */
    private Integer residentId;

    /**
     * Full name of the family member.
     *
     * <p>Maps from {@code family_members.member_name}
     * (VARCHAR(100) → String).
     */
    private String memberName;

    /**
     * Relationship of this member to the resident.
     *
     * <p>Maps from {@code family_members.relation}
     * (ENUM('FATHER','MOTHER','SPOUSE','SON','DAUGHTER',
     * 'BROTHER','SISTER','OTHER') → RelationType).
     */
    private RelationType relation;

    /**
     * Age of the family member in years.
     *
     * <p>Maps from {@code family_members.age}
     * (TINYINT UNSIGNED → Short).
     *
     * <p>{@code Short} safely covers TINYINT UNSIGNED range (0–255).
     * Nullable — age is not mandatory.
     */
    private Byte age;

    /**
     * Mobile number of the family member.
     *
     * <p>Maps from {@code family_members.mobile}
     * (CHAR(10) → String).
     * Nullable — mobile is not mandatory for family members.
     */
    private String mobile;

    /**
     * Timestamp when this family member record was created.
     *
     * <p>Maps from {@code family_members.created_at}
     * (TIMESTAMP DEFAULT CURRENT_TIMESTAMP → LocalDateTime).
     */
    private LocalDateTime createdAt;
}