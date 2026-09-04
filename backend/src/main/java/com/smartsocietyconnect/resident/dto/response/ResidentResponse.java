package com.smartsocietyconnect.resident.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.smartsocietyconnect.resident.entity.ResidentType;

import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO returned after resident-related operations.
 *
 * <p>Represents a flattened, read-only snapshot combining data from
 * three tables — {@code residents}, {@code users}, and {@code flats} —
 * into a single response object for the client.
 *
 * <p>This is an outbound-only DTO — never deserialized from client requests.
 * {@code @Builder} is sufficient; no {@code @NoArgsConstructor} or
 * {@code @AllArgsConstructor} is needed since Jackson only serializes
 * (writes) this object to JSON.
 *
 * <p>Field types match their source entity fields exactly to avoid
 * silent type conversion errors during mapping.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see com.smartsocietyconnect.resident.entity.Resident
 * @see ResidentType
 */
@Getter
@Builder
public class ResidentResponse {

    // =========================================================================
    // Resident Core
    // =========================================================================

    /**
     * Unique identifier of the resident profile.
     *
     * <p>Maps from {@code residents.resident_id} (INT UNSIGNED → Integer).
     */
    private Integer residentId;

    // =========================================================================
    // User Information (from users table)
    // =========================================================================

    /**
     * ID of the linked user account.
     *
     * <p>Maps from {@code users.user_id} (INT UNSIGNED → Integer).
     * One user maps to exactly one resident profile.
     */
    private Integer userId;

    /**
     * User's first name.
     *
     * <p>Maps from {@code users.first_name} (VARCHAR(30) → String).
     */
    private String firstName;

    /**
     * User's last name.
     *
     * <p>Maps from {@code users.last_name} (VARCHAR(30) → String).
     */
    private String lastName;

    /**
     * User's registered email address.
     *
     * <p>Maps from {@code users.email} (VARCHAR(100) → String).
     */
    private String email;

    /**
     * User's 10-digit mobile number.
     *
     * <p>Maps from {@code users.mobile} (CHAR(10) → String).
     */
    private String mobile;

    // =========================================================================
    // Flat Information (from flats table)
    // =========================================================================

    /**
     * ID of the flat assigned to this resident.
     *
     * <p>Maps from {@code flats.flat_id} (SMALLINT UNSIGNED → Integer).
     */
    private Integer flatId;

    /**
     * Unique alphanumeric flat number.
     *
     * <p>Maps from {@code flats.flat_number} (VARCHAR(10) → String).
     * Examples: {@code "A101"}, {@code "B203"}
     */
    private String flatNumber;

    /**
     * Wing/block letter of the building.
     *
     * <p>Maps from {@code flats.wing} (CHAR(1) → String).
     * Examples: {@code "A"}, {@code "B"}, {@code "C"}
     */
    private String wing;

    // =========================================================================
    // Resident Information (from residents table)
    // =========================================================================

    /**
     * Whether the resident owns or rents the flat.
     *
     * <p>Maps from {@code residents.resident_type}
     * (ENUM('OWNER','TENANT') → ResidentType).
     */
    private ResidentType residentType;

    /**
     * Resident's occupation or profession.
     *
     * <p>Maps from {@code residents.occupation} (VARCHAR(100) → String).
     * Nullable — not mandatory during registration.
     */
    private String occupation;

    /**
     * Emergency contact mobile number for the resident.
     *
     * <p>Maps from {@code residents.emergency_contact} (CHAR(10) → String).
     * Nullable — not mandatory during registration.
     */
    private String emergencyContact;

    /**
     * Date when the resident moved into the flat.
     *
     * <p>Maps from {@code residents.move_in_date} (DATE → LocalDate).
     * Required — all residents must have a move-in date.
     */
    private LocalDate moveInDate;

    /**
     * Date when the resident moved out of the flat.
     *
     * <p>Maps from {@code residents.move_out_date} (DATE → LocalDate).
     * Nullable — null means the resident is still active in the flat.
     */
    private LocalDate moveOutDate;

    /**
     * Whether this resident is the primary member of the flat.
     *
     * <p>Maps from {@code residents.is_primary_member} (BOOLEAN → Boolean).
     * Defaults to {@code true} for the first registered resident of a flat.
     * Additional family members or tenants may have this set to {@code false}.
     */
    private Boolean isPrimaryMember;

    /**
     * Timestamp when the resident profile was created.
     *
     * <p>Maps from {@code residents.created_at}
     * (TIMESTAMP DEFAULT CURRENT_TIMESTAMP → LocalDateTime).
     */
    private LocalDateTime createdAt;
}