package com.smartsocietyconnect.resident.dto.request;

import com.smartsocietyconnect.resident.entity.FlatType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for creating a new flat in Smart Society Connect.
 *
 * <p>Carries the input data from the client for the flat registration endpoint.
 * Only creation-time fields are included — {@code status} and {@code createdAt}
 * are intentionally excluded:
 * <ul>
 *     <li>{@code status} — defaults to {@code VACANT} in the entity;
 *         clients cannot set it at creation time</li>
 *     <li>{@code createdAt} — set automatically by Hibernate via
 *         {@code @CreationTimestamp}; never client-supplied</li>
 * </ul>
 *
 * <p>All field-level constraints mirror the DB column definitions to ensure
 * validation failures are caught at the DTO layer and returned as
 * {@code 400 Bad Request}, rather than reaching the DB and causing
 * unhandled {@code DataIntegrityViolationException} (500 errors).
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
@Getter
@Setter
public class CreateFlatRequest {

    /**
     * Unique alphanumeric identifier for the flat (e.g. {@code "A101"}, {@code "B-203"}).
     *
     * <p>Maps to {@code flat_number} column (VARCHAR(10), NOT NULL, UNIQUE).
     *
     * <p>{@code @Size(max = 10)} mirrors the {@code VARCHAR(10)} DB constraint.
     * Without it, a value longer than 10 characters bypasses validation,
     * reaches the DB, and causes an unhandled {@code DataIntegrityViolationException}.
     */
    @NotBlank(message = "Flat number is required")
    @Size(max = 10, message = "Flat number must not exceed 10 characters")
    private String flatNumber;

    /**
     * Wing or block letter of the building (e.g. {@code "A"}, {@code "B"}).
     *
     * <p>Maps to {@code wing} column (CHAR(1), NOT NULL).
     * Must be exactly one uppercase letter A–Z.
     */
    @NotBlank(message = "Wing is required")
    @Pattern(
            regexp = "^[A-Z]$",
            message = "Wing must be a single uppercase letter (A–Z)"
    )
    private String wing;

    /**
     * Floor number on which the flat is located.
     *
     * <p>Maps to {@code floor_number} column (TINYINT UNSIGNED, NOT NULL).
     * Valid range is 0–255 (ground floor = 0), matching the TINYINT UNSIGNED
     * DB constraint.
     *
     * <p>{@code Integer} is used in the DTO (rather than {@code Short}) because
     * JSON numeric values deserialize to {@code Integer} by default in Jackson.
     * The service layer is responsible for converting to {@code Short} when
     * building the {@link com.smartsocietyconnect.resident.entity.Flat} entity.
     */
    @NotNull(message = "Floor number is required")
    @Min(value = 0, message = "Floor number cannot be negative")
    @Max(value = 255, message = "Floor number cannot exceed 255 (TINYINT UNSIGNED limit)")
    private Integer floorNumber;

    /**
     * Configuration type of the flat (e.g. {@code 1BHK}, {@code 2BHK}).
     *
     * <p>Maps to {@code flat_type} column
     * ({@code ENUM('1RK','1BHK','2BHK','3BHK','4BHK')}, NOT NULL).
     * Jackson deserializes the JSON string to the {@link FlatType} enum constant
     * by name; an unrecognised value results in a deserialization error.
     */
    @NotNull(message = "Flat type is required")
    private FlatType flatType;

    /**
     * Total area of the flat in square feet.
     *
     * <p>Maps to {@code area_sqft} column (SMALLINT UNSIGNED, nullable).
     * Optional — may be omitted if the area is not known at registration time.
     *
     * <p>{@code @Min(100)} ensures physically implausible areas are rejected.
     * {@code @Max(65535)} mirrors the SMALLINT UNSIGNED DB constraint (0–65,535).
     * Without {@code @Max}, a value like {@code 1,000,000} passes validation,
     * reaches the DB, and causes an unhandled {@code DataTruncation} exception.
     */
    @Min(value = 100, message = "Area must be at least 100 sqft")
    @Max(value = 65535, message = "Area cannot exceed 65,535 sqft (SMALLINT UNSIGNED limit)")
    private Integer areaSqft;

}