package com.smartsocietyconnect.resident.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartsocietyconnect.common.payload.ApiResponse;
import com.smartsocietyconnect.resident.dto.request.CreateResidentRequest;
import com.smartsocietyconnect.resident.dto.request.UpdateResidentRequest;
import com.smartsocietyconnect.resident.dto.response.ResidentResponse;
import com.smartsocietyconnect.resident.service.ResidentService;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for resident management operations.
 *
 * <p>Exposes APIs for creating resident profiles, fetching residents,
 * updating resident details, moving residents out, and deleting resident records.
 *
 * <p>This controller stays thin by handling only HTTP request/response mapping.
 * Business rules such as duplicate user checks, flat occupancy updates,
 * primary member handling, and move-out validation belong in {@link ResidentService}.
 */
@Tag(
        name = "Resident Management",
        description = "APIs for managing resident profiles and move-out operations"
)
@Validated
@RestController
@RequestMapping("/api/residents")
@RequiredArgsConstructor
public class ResidentController {

    private final ResidentService residentService;

    // ==========================================================================
    // 1. Create Resident - POST /api/residents
    // ==========================================================================

    /**
     * Creates a new resident profile.
     *
     * <p>The request body is validated using {@code @Valid}. The service layer
     * verifies that the user and flat exist, prevents duplicate resident profiles,
     * and updates flat occupancy status if required.
     *
     * @param request resident creation request body
     * @return created resident wrapped in standard API response
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ResidentResponse>> createResident(
            @Valid @RequestBody CreateResidentRequest request
    ) {
        ResidentResponse response = residentService.createResident(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(success("Resident created successfully", response));
    }

    // ==========================================================================
    // 2. Get Resident By ID - GET /api/residents/{residentId}
    // ==========================================================================

    /**
     * Fetches a resident by resident ID.
     *
     * @param residentId resident primary key
     * @return resident details wrapped in standard API response
     */
    @GetMapping("/{residentId}")
    public ResponseEntity<ApiResponse<ResidentResponse>> getResidentById(
            @Positive(message = "Resident ID must be positive")
            @PathVariable Integer residentId
    ) {
        ResidentResponse response = residentService.getResidentById(residentId);

        return ResponseEntity.ok(
                success("Resident fetched successfully", response)
        );
    }

    // ==========================================================================
    // 3. Get All Residents - GET /api/residents
    // ==========================================================================

    /**
     * Fetches all resident profiles.
     *
     * <p>This returns both active and moved-out residents unless the service layer
     * applies additional filtering.
     *
     * @return list of residents wrapped in standard API response
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ResidentResponse>>> getAllResidents() {
        List<ResidentResponse> response = residentService.getAllResidents();

        return ResponseEntity.ok(
                success("Residents fetched successfully", response)
        );
    }

    // ==========================================================================
    // 4. Get Resident By User ID - GET /api/residents/user/{userId}
    // ==========================================================================

    /**
     * Fetches the resident profile linked to a user account.
     *
     * <p>Each user can have at most one resident profile because the resident
     * entity uses a one-to-one user mapping.
     *
     * @param userId linked user account ID
     * @return resident details wrapped in standard API response
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY') or @ownershipAuthorization.isCurrentUser(#p0, authentication)")
    public ResponseEntity<ApiResponse<ResidentResponse>> getResidentByUserId(
            @Positive(message = "User ID must be positive")
            @PathVariable Integer userId
    ) {
        ResidentResponse response = residentService.getResidentByUserId(userId);

        return ResponseEntity.ok(
                success("Resident fetched successfully", response)
        );
    }

    // ==========================================================================
    // 5. Get Residents By Flat - GET /api/residents/flat/{flatId}
    // ==========================================================================

    /**
     * Fetches residents linked to a flat.
     *
     * <p>Depending on service implementation, this may include all historical
     * residents or only active residents. Keep that behavior consistent with
     * the repository method used in the service layer.
     *
     * @param flatId flat primary key
     * @return list of residents for the flat
     */
    @GetMapping("/flat/{flatId}")
    public ResponseEntity<ApiResponse<List<ResidentResponse>>> getResidentsByFlat(
            @Positive(message = "Flat ID must be positive")
            @PathVariable Integer flatId
    ) {
        List<ResidentResponse> response = residentService.getResidentsByFlat(flatId);

        return ResponseEntity.ok(
                success("Residents fetched successfully", response)
        );
    }

    // ==========================================================================
    // 6. Update Resident - PUT /api/residents/{residentId}
    // ==========================================================================

    /**
     * Updates an existing resident profile.
     *
     * <p>The update request is designed for partial updates. The service layer
     * should apply only non-null fields and preserve existing values for fields
     * not provided by the client.
     *
     * @param residentId resident primary key
     * @param request resident update request body
     * @return updated resident wrapped in standard API response
     */
    @PutMapping("/{residentId}")
    public ResponseEntity<ApiResponse<ResidentResponse>> updateResident(
            @Positive(message = "Resident ID must be positive")
            @PathVariable Integer residentId,
            @Valid @RequestBody UpdateResidentRequest request
    ) {
        ResidentResponse response = residentService.updateResident(residentId, request);

        return ResponseEntity.ok(
                success("Resident updated successfully", response)
        );
    }

    // ==========================================================================
    // 7. Move Out Resident - PATCH /api/residents/{residentId}/move-out
    // ==========================================================================

    /**
     * Marks a resident as moved out.
     *
     * <p>The service layer should set the move-out date, update primary member
     * status if needed, and mark the flat as vacant when no active residents remain.
     *
     * @param residentId resident primary key
     * @return moved-out resident wrapped in standard API response
     */
    @PatchMapping("/{residentId}/move-out")
    public ResponseEntity<ApiResponse<ResidentResponse>> moveOutResident(
            @Positive(message = "Resident ID must be positive")
            @PathVariable Integer residentId
    ) {
        ResidentResponse response = residentService.moveOutResident(residentId);

        return ResponseEntity.ok(
                success("Resident moved out successfully", response)
        );
    }

    // ==========================================================================
    // 8. Delete Resident - DELETE /api/residents/{residentId}
    // ==========================================================================

    /**
     * Deletes a resident profile.
     *
     * <p>The service layer should handle related business rules, such as updating
     * flat occupancy if this was the last active resident in the flat.
     *
     * @param residentId resident primary key
     * @return success response with no data
     */
    @DeleteMapping("/{residentId}")
    public ResponseEntity<ApiResponse<Void>> deleteResident(
            @Positive(message = "Resident ID must be positive")
            @PathVariable Integer residentId
    ) {
        residentService.deleteResident(residentId);

        return ResponseEntity.ok(
                success("Resident deleted successfully", null)
        );
    }

    // ==========================================================================
    // Private Helpers
    // ==========================================================================

    /**
     * Builds a standard successful API response.
     *
     * <p>Centralizing response creation keeps controller methods readable and
     * ensures every success response has the same structure.
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
