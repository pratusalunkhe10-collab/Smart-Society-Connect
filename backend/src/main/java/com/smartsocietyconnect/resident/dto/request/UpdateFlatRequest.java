package com.smartsocietyconnect.resident.dto.request;

import com.smartsocietyconnect.resident.entity.FlatStatus;
import com.smartsocietyconnect.resident.entity.FlatType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for partially updating an existing flat's information.
 *
 * <p>All fields in this request are <strong>optional</strong> to support PATCH/UPDATE
 * operations where clients can modify only specific attributes without sending the
 * entire flat payload.</p>
 *
 * <p><strong>Validation Constraints:</strong> Aligned with the database schema
 * (Design 3) to ensure data integrity and memory optimization before hitting the
 * persistence layer.</p>
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @since 2026-06-01
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateFlatRequest {

    /**
     * The unique alphanumeric identifier for the flat (e.g., "A101", "B204").
     *
     * <p><strong>Constraints:</strong></p>
     * <ul>
     *   <li>Maximum length: 10 characters (matches {@code VARCHAR(10)} in DB)</li>
     *   <li>Must remain unique across the database, though uniqueness is enforced at the entity level</li>
     * </ul>
     */
    @Size(max = 10, message = "Flat number must not exceed 10 characters")
    private String flatNumber;

    /**
     * The building wing designation where the flat is located.
     *
     * <p><strong>Constraints:</strong></p>
     * <ul>
     *   <li>Must be a single uppercase letter (e.g., 'A', 'B', 'C', 'D')</li>
     *   <li>Mapped to {@code CHAR(1)} in the database for fixed-length efficiency</li>
     * </ul>
     */
    @Pattern(regexp = "^[A-Z]$", message = "Wing must be a single uppercase letter")
    private String wing;

    /**
     * The floor number on which the flat is situated.
     *
     * <p><strong>Business &amp; DB Alignment:</strong></p>
     * <ul>
     *   <li>Minimum: 0 (Ground floor)</li>
     *   <li>Maximum: 255 (fits perfectly into the {@code TINYINT UNSIGNED} database type, saving memory)</li>
     * </ul>
     */
    @Min(value = 0, message = "Floor number cannot be negative")
    @Max(value = 255, message = "Floor number cannot exceed 255 (database TINYINT limit)")
    private Integer floorNumber;

    /**
     * The structural configuration of the flat (e.g., 1RK, 2BHK, 3BHK).
     *
     * <p>This is a strict enum mapping to the {@code ENUM(...)} column in the database,
     * ensuring only valid flat types are persisted.</p>
     */
    private FlatType flatType;

    /**
     * The total carpet area of the flat in square feet.
     *
     * <p><strong>Validation Rationale:</strong></p>
     * <ul>
     *   <li>Minimum: 100 sqft (practical lower bound for a habitable flat)</li>
     *   <li>Maximum: 65,535 sqft (matches the {@code SMALLINT UNSIGNED} DB type used in Design 3,
     *   optimizing storage to just 2 bytes)</li>
     * </ul>
     */
    @Min(value = 100, message = "Area must be at least 100 sqft")
    @Max(value = 65535, message = "Area cannot exceed 65535 sqft (database SMALLINT limit)")
    private Integer areaSqft;

    /**
     * The current occupancy status of the flat (e.g., OCCUPIED or VACANT).
     *
     * <p>Updating this status is typically triggered when a resident moves in or out,
     * and is used by the {@code ResidentService} for flat allocation logic.</p>
     */
    private FlatStatus status;
}