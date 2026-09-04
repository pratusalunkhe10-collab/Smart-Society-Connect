package com.smartsocietyconnect.resident.dto.request;

import java.time.LocalDate;

import com.smartsocietyconnect.resident.entity.ResidentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for registering a new resident in Smart Society Connect.
 *
 * <p>Carries the input data from the client for the resident registration endpoint.
 * The following fields are intentionally excluded:
 * <ul>
 *     <li>{@code residentId} — auto-generated primary key; never client-supplied</li>
 *     <li>{@code moveOutDate} — logically impossible at creation time; a resident
 *         cannot move out at the same moment they move in. Use a dedicated
 *         {@code MoveOutRequest} or {@code UpdateResidentRequest} to record
 *         departure</li>
 *     <li>{@code createdAt} — set automatically by Hibernate via
 *         {@code @CreationTimestamp}</li>
 * </ul>
 *
 * <p>All field-level constraints mirror the DB column definitions to ensure
 * validation failures are caught at the DTO layer and returned as
 * {@code 400 Bad Request}, rather than reaching the DB and causing
 * unhandled {@code DataIntegrityViolationException} (500 errors).
 *
 * <p><b>Security note on {@code userId}:</b> In a self-registration flow,
 * {@code userId} should be extracted from the JWT security context in the
 * service layer rather than trusted from the request body — a client could
 * otherwise supply a different user's ID and create a resident profile on
 * their behalf. In an admin-initiated registration flow, body-supplied
 * {@code userId} is appropriate.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
@Getter
@Setter
public class CreateResidentRequest {

    /**
     * ID of the user account to link this resident profile to.
     *
     * <p>References {@code users.user_id}. The user must exist before
     * a resident profile can be created.
     *
     * <p>See class-level security note regarding {@code userId} trust in
     * self-registration vs admin-initiated flows.
     */
    @NotNull(message = "User ID is required")
    private Integer userId;

    /**
     * ID of the flat the resident is moving into.
     *
     * <p>References {@code flats.flat_id}. The flat must exist and
     * should be in {@code VACANT} status before assignment.
     */
    @NotNull(message = "Flat ID is required")
    private Integer flatId;

    /**
     * Whether the resident is an owner or tenant.
     *
     * <p>Maps to {@code resident_type} column
     * ({@code ENUM('OWNER','TENANT')}, NOT NULL).
     */
    @NotNull(message = "Resident type is required")
    private ResidentType residentType;

    /**
     * The resident's occupation or profession (e.g. {@code "Software Engineer"}).
     *
     * <p>Maps to {@code occupation} column (VARCHAR(100), nullable).
     * Optional — may be omitted if not known.
     *
     * <p>{@code @Size(max = 100)} mirrors the {@code VARCHAR(100)} DB constraint.
     * Without it, a value longer than 100 characters bypasses validation,
     * reaches the DB, and causes an unhandled {@code DataIntegrityViolationException}.
     */
    @Size(max = 100, message = "Occupation must not exceed 100 characters")
    private String occupation;

    /**
     * Emergency contact mobile number for this resident.
     *
     * <p>Maps to {@code emergency_contact} column (CHAR(10), nullable).
     * Optional — must be exactly 10 digits if provided.
     */
    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "Emergency contact must be exactly 10 digits"
    )
    private String emergencyContact;

    /**
     * Date the resident is moving into the flat.
     *
     * <p>Maps to {@code move_in_date} column (DATE, NOT NULL).
     * Must be today or a date in the past — future move-in dates are
     * not accepted. For pre-registration of upcoming residents,
     * consider a separate pre-registration endpoint.
     */
    @NotNull(message = "Move-in date is required")
    @PastOrPresent(message = "Move-in date cannot be in the future")
    private LocalDate moveInDate;

    /**
     * Whether this resident is the primary member of the flat.
     *
     * <p>Maps to {@code is_primary_member} column (TINYINT(1), DEFAULT 1).
     * Optional — if omitted ({@code null}), the service layer must default
     * this to {@code true}. Each flat should have exactly one primary member
     * at any given time.
     */
    private Boolean isPrimaryMember;

}