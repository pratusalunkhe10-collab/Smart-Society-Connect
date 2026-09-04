package com.smartsocietyconnect.resident.dto.response;

import java.time.LocalDateTime;

import com.smartsocietyconnect.resident.entity.FlatStatus;
import com.smartsocietyconnect.resident.entity.FlatType;

import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO returned after flat-related operations.
 *
 * <p>Represents a read-only snapshot of a {@code flats} table record.
 *
 * <p>This DTO is used only for API responses. It should not be used as an
 * input/request DTO.
 */
@Getter
@Builder
public class FlatResponse {

    /**
     * Unique flat ID.
     *
     * <p>Maps from {@code Flat.flatId}.
     */
    private Integer flatId;

    /**
     * Unique flat number.
     */
    private String flatNumber;

    /**
     * Wing/block of the flat.
     */
    private String wing;

    /**
     * Floor number.
     */
    private Short floorNumber;

    /**
     * Flat configuration type.
     */
    private FlatType flatType;

    /**
     * Flat area in square feet.
     */
    private Integer areaSqft;

    /**
     * Current occupancy status.
     */
    private FlatStatus status;

    /**
     * Creation timestamp.
     */
    private LocalDateTime createdAt;
}