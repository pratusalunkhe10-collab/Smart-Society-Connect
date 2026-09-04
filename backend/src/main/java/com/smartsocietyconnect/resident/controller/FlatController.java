package com.smartsocietyconnect.resident.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartsocietyconnect.common.payload.ApiResponse;
import com.smartsocietyconnect.resident.dto.request.CreateFlatRequest;
import com.smartsocietyconnect.resident.dto.request.UpdateFlatRequest;
import com.smartsocietyconnect.resident.dto.response.FlatResponse;
import com.smartsocietyconnect.resident.entity.FlatStatus;
import com.smartsocietyconnect.resident.entity.FlatType;
import com.smartsocietyconnect.resident.service.FlatService;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for flat management operations.
 *
 * <p>Exposes APIs for creating, fetching, filtering, updating, changing status,
 * and deleting flats.
 *
 * <p>This controller is intentionally thin. It only handles HTTP request/response
 * mapping and delegates all business logic to {@link FlatService}.
 */
@Tag(
        name = "Flat Management",
        description = "APIs for managing society flats and flat occupancy status"
)
@Validated
@RestController
@RequestMapping("/api/flats")
@RequiredArgsConstructor
public class FlatController {

    private final FlatService flatService;

    // ==========================================================================
    // 1. Create Flat - POST /api/flats
    // ==========================================================================

    /**
     * Creates a new flat.
     *
     * <p>Request validation is handled using {@code @Valid}. Duplicate flat
     * number checks and persistence logic are handled in the service layer.
     *
     * @param request flat creation request body
     * @return created flat wrapped in standard API response
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FlatResponse>> createFlat(
            @Valid @RequestBody CreateFlatRequest request
    ) {
        FlatResponse response = flatService.createFlat(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(success("Flat created successfully", response));
    }

    // ==========================================================================
    // 2. Get Flat By ID - GET /api/flats/{flatId}
    // ==========================================================================

    /**
     * Fetches a flat by its unique ID.
     *
     * @param flatId flat primary key
     * @return flat details wrapped in standard API response
     */
    @GetMapping("/{flatId}")
    public ResponseEntity<ApiResponse<FlatResponse>> getFlatById(
            @Positive(message = "Flat ID must be positive")
            @PathVariable Integer flatId
    ) {
        FlatResponse response = flatService.getFlatById(flatId);

        return ResponseEntity.ok(
                success("Flat fetched successfully", response)
        );
    }

    // ==========================================================================
    // 3. Get All Flats - GET /api/flats
    // ==========================================================================

    /**
     * Fetches all flats.
     *
     * @return list of all flats wrapped in standard API response
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FlatResponse>>> getAllFlats() {
        List<FlatResponse> response = flatService.getAllFlats();

        return ResponseEntity.ok(
                success("Flats fetched successfully", response)
        );
    }

    // ==========================================================================
    // 4. Get Flats By Wing - GET /api/flats/wing/{wing}
    // ==========================================================================

    /**
     * Fetches all flats belonging to a specific wing.
     *
     * <p>Wing is validated at controller level to ensure only a single uppercase
     * letter is accepted before the service/repository layer is called.
     *
     * @param wing wing identifier, for example A, B, C
     * @return list of flats in the requested wing
     */
    @GetMapping("/wing/{wing}")
    public ResponseEntity<ApiResponse<List<FlatResponse>>> getFlatsByWing(
            @Pattern(
                    regexp = "^[A-Z]$",
                    message = "Wing must be a single uppercase letter"
            )
            @PathVariable String wing
    ) {
        List<FlatResponse> response = flatService.getFlatsByWing(wing);

        return ResponseEntity.ok(
                success("Wing flats fetched successfully", response)
        );
    }

    // ==========================================================================
    // 5. Get Flats By Status - GET /api/flats/status/{status}
    // ==========================================================================

    /**
     * Fetches flats by occupancy status.
     *
     * <p>Spring automatically converts the path variable to {@link FlatStatus}.
     * Invalid enum values are handled by the global exception handler.
     *
     * @param status flat status, for example VACANT or OCCUPIED
     * @return list of flats matching the requested status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<FlatResponse>>> getFlatsByStatus(
            @PathVariable FlatStatus status
    ) {
        List<FlatResponse> response = flatService.getFlatsByStatus(status);

        return ResponseEntity.ok(
                success("Status flats fetched successfully", response)
        );
    }

    // ==========================================================================
    // 6. Get Flats By Type - GET /api/flats/type/{flatType}
    // ==========================================================================

    /**
     * Fetches flats by flat type.
     *
     * <p>Spring automatically converts the path variable to {@link FlatType}.
     *
     * @param flatType flat type, for example _1BHK, _2BHK, _3BHK
     * @return list of flats matching the requested type
     */
    @GetMapping("/type/{flatType}")
    public ResponseEntity<ApiResponse<List<FlatResponse>>> getFlatsByType(
            @PathVariable FlatType flatType
    ) {
        List<FlatResponse> response = flatService.getFlatsByType(flatType);

        return ResponseEntity.ok(
                success("Flat type fetched successfully", response)
        );
    }

    // ==========================================================================
    // 7. Update Flat - PUT /api/flats/{flatId}
    // ==========================================================================

    /**
     * Updates an existing flat.
     *
     * <p>The DTO supports partial updates. Only non-null fields should be applied
     * by the service layer.
     *
     * @param flatId flat primary key
     * @param request flat update request body
     * @return updated flat wrapped in standard API response
     */
    @PutMapping("/{flatId}")
    public ResponseEntity<ApiResponse<FlatResponse>> updateFlat(
            @Positive(message = "Flat ID must be positive")
            @PathVariable Integer flatId,
            @Valid @RequestBody UpdateFlatRequest request
    ) {
        FlatResponse response = flatService.updateFlat(flatId, request);

        return ResponseEntity.ok(
                success("Flat updated successfully", response)
        );
    }

    // ==========================================================================
    // 8. Update Flat Status - PATCH /api/flats/{flatId}/status
    // ==========================================================================

    /**
     * Updates only the occupancy status of a flat.
     *
     * <p>This endpoint is useful for admin corrections. Normal status changes
     * may also happen automatically when residents move in or move out.
     *
     * @param flatId flat primary key
     * @param status new flat status
     * @return updated flat wrapped in standard API response
     */
    @PatchMapping("/{flatId}/status")
    public ResponseEntity<ApiResponse<FlatResponse>> updateFlatStatus(
            @Positive(message = "Flat ID must be positive")
            @PathVariable Integer flatId,
            @RequestParam FlatStatus status
    ) {
        FlatResponse response = flatService.updateFlatStatus(flatId, status);

        return ResponseEntity.ok(
                success("Flat status updated successfully", response)
        );
    }

    // ==========================================================================
    // 9. Delete Flat - DELETE /api/flats/{flatId}
    // ==========================================================================

    /**
     * Deletes a flat by ID.
     *
     * <p>The service layer should prevent deletion of occupied flats or flats
     * linked to active residents.
     *
     * @param flatId flat primary key
     * @return success response with no data
     */
    @DeleteMapping("/{flatId}")
    public ResponseEntity<ApiResponse<Void>> deleteFlat(
            @Positive(message = "Flat ID must be positive")
            @PathVariable Integer flatId
    ) {
        flatService.deleteFlat(flatId);

        return ResponseEntity.ok(
                success("Flat deleted successfully", null)
        );
    }

    // ==========================================================================
    // Private Helpers
    // ==========================================================================

    /**
     * Builds a standard successful API response.
     *
     * <p>Centralizing response creation keeps controller methods short and
     * ensures all success responses have the same structure.
     *
     * @param message success message
     * @param data response payload
     * @param <T> response data type
     * @return standardized API response
     */
    private <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
}