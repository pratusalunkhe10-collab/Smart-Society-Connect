package com.smartsocietyconnect.visitor.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.smartsocietyconnect.common.payload.ApiResponse;
import com.smartsocietyconnect.visitor.dto.VisitorApprovalDTO;
import com.smartsocietyconnect.visitor.dto.VisitorCheckInDTO;
import com.smartsocietyconnect.visitor.dto.VisitorCheckOutDTO;
import com.smartsocietyconnect.visitor.dto.VisitorRequestDTO;
import com.smartsocietyconnect.visitor.dto.VisitorResponseDTO;
import com.smartsocietyconnect.visitor.enums.VisitorStatus;
import com.smartsocietyconnect.visitor.service.VisitorService;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for visitor management operations.
 *
 * <p>Exposes APIs for visitor creation, approval/rejection, check-in,
 * check-out, visitor lookup, visitor filtering, and deletion.
 *
 * <p>This controller is intentionally thin. It handles HTTP routing,
 * request validation, and API response wrapping. Business rules such as
 * valid visitor status transitions are handled inside {@link VisitorService}.
 */
@Tag(
        name = "Visitor Management",
        description = "APIs for managing visitor requests, approvals, check-in, and check-out"
)
@Validated
@RestController
@RequestMapping("/api/visitors")
@RequiredArgsConstructor
public class VisitorController {

    private final VisitorService visitorService;

    // ==========================================================================
    // 1. Create Visitor - POST /api/visitors
    // ==========================================================================

    /**
     * Creates a new visitor request.
     *
     * <p>New visitors should start with {@code REQUESTED} status in the
     * service layer.
     *
     * @param request visitor creation request body
     * @return created visitor wrapped in standard API response
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECURITY', 'SECRETARY') or @ownershipAuthorization.isCurrentResident(#p0.residentId, authentication)")
    public ResponseEntity<ApiResponse<VisitorResponseDTO>> createVisitor(
            @Valid @RequestBody VisitorRequestDTO request
    ) {
        VisitorResponseDTO response = visitorService.createVisitor(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(success("Visitor request created successfully", response));
    }

    /** Uploads an optional visitor photo for gate identification. */
    @PostMapping(value = "/{visitorId}/photo", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY', 'SECURITY') or @ownershipAuthorization.ownsVisitor(#visitorId, authentication)")
    public ResponseEntity<ApiResponse<VisitorResponseDTO>> uploadVisitorPhoto(
            @Positive(message = "Visitor ID must be positive") @PathVariable Integer visitorId,
            @RequestParam("photo") MultipartFile photo
    ) {
        return ResponseEntity.ok(success(
                "Visitor photo uploaded successfully",
                visitorService.updateVisitorPhoto(visitorId, photo)
        ));
    }

    @GetMapping("/{visitorId}/photo")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY', 'SECURITY') or @ownershipAuthorization.ownsVisitor(#visitorId, authentication)")
    public ResponseEntity<Resource> getVisitorPhoto(
            @Positive(message = "Visitor ID must be positive") @PathVariable Integer visitorId
    ) {
        VisitorResponseDTO visitor = visitorService.getVisitorById(visitorId);
        String path = visitor.photoUrl() == null ? "" : visitor.photoUrl().toLowerCase();
        MediaType mediaType = path.endsWith(".png") ? MediaType.IMAGE_PNG
                : path.endsWith(".webp") ? MediaType.parseMediaType("image/webp")
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(mediaType).body(visitorService.loadVisitorPhoto(visitorId));
    }

    // ==========================================================================
    // 2. Approve / Reject Visitor - PUT /api/visitors/{visitorId}/approve
    // ==========================================================================

    /**
     * Approves or rejects a visitor request.
     *
     * <p>If {@code approved = true}, visitor status becomes {@code APPROVED}.
     * If {@code approved = false}, visitor status becomes {@code REJECTED}.
     *
     * @param visitorId visitor primary key
     * @param request approval decision request body
     * @return updated visitor wrapped in standard API response
     */
    @PutMapping("/{visitorId}/approve")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY') or @ownershipAuthorization.ownsVisitor(#p0, authentication)")
    public ResponseEntity<ApiResponse<VisitorResponseDTO>> approveVisitor(
            @Positive(message = "Visitor ID must be positive")
            @PathVariable Integer visitorId,
            @Valid @RequestBody VisitorApprovalDTO request
    ) {
        VisitorResponseDTO response =
                visitorService.approveVisitor(visitorId, request);

        return ResponseEntity.ok(
                success("Visitor approval decision updated successfully", response)
        );
    }

    // ==========================================================================
    // 3. Check-In Visitor - PUT /api/visitors/{visitorId}/check-in
    // ==========================================================================

    /**
     * Checks in an approved visitor.
     *
     * <p>Only visitors with {@code APPROVED} status should be allowed to
     * check in. The service layer sets actual check-in time using server time.
     *
     * @param visitorId visitor primary key
     * @param request check-in request body
     * @return checked-in visitor wrapped in standard API response
     */
    @PutMapping("/{visitorId}/check-in")
    public ResponseEntity<ApiResponse<VisitorResponseDTO>> checkInVisitor(
            @Positive(message = "Visitor ID must be positive")
            @PathVariable Integer visitorId,
            @Valid @RequestBody VisitorCheckInDTO request
    ) {
        VisitorResponseDTO response =
                visitorService.checkInVisitor(visitorId, request);

        return ResponseEntity.ok(
                success("Visitor checked in successfully", response)
        );
    }

    // ==========================================================================
    // 4. Check-Out Visitor - PUT /api/visitors/{visitorId}/check-out
    // ==========================================================================

    /**
     * Checks out a checked-in visitor.
     *
     * <p>Only visitors with {@code CHECKED_IN} status should be allowed to
     * check out. The service layer sets actual check-out time using server time.
     *
     * @param visitorId visitor primary key
     * @param request check-out request body
     * @return checked-out visitor wrapped in standard API response
     */
    @PutMapping("/{visitorId}/check-out")
    public ResponseEntity<ApiResponse<VisitorResponseDTO>> checkOutVisitor(
            @Positive(message = "Visitor ID must be positive")
            @PathVariable Integer visitorId,
            @Valid @RequestBody VisitorCheckOutDTO request
    ) {
        VisitorResponseDTO response =
                visitorService.checkOutVisitor(visitorId, request);

        return ResponseEntity.ok(
                success("Visitor checked out successfully", response)
        );
    }

    // ==========================================================================
    // 5. Get Visitor By ID - GET /api/visitors/{visitorId}
    // ==========================================================================

    /**
     * Fetches a visitor by visitor ID.
     *
     * @param visitorId visitor primary key
     * @return visitor details wrapped in standard API response
     */
    @GetMapping("/{visitorId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECURITY', 'SECRETARY') or @ownershipAuthorization.ownsVisitor(#p0, authentication)")
    public ResponseEntity<ApiResponse<VisitorResponseDTO>> getVisitorById(
            @Positive(message = "Visitor ID must be positive")
            @PathVariable Integer visitorId
    ) {
        VisitorResponseDTO response = visitorService.getVisitorById(visitorId);

        return ResponseEntity.ok(
                success("Visitor fetched successfully", response)
        );
    }

    // ==========================================================================
    // 6. Get All Visitors - GET /api/visitors
    // ==========================================================================

    /**
     * Fetches all visitor records.
     *
     * @return list of visitors wrapped in standard API response
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECURITY', 'SECRETARY')")
    public ResponseEntity<ApiResponse<List<VisitorResponseDTO>>> getAllVisitors() {
        List<VisitorResponseDTO> response = visitorService.getAllVisitors();

        return ResponseEntity.ok(
                success("Visitors fetched successfully", response)
        );
    }

    // ==========================================================================
    // 7. Get Visitors By Resident - GET /api/visitors/resident/{residentId}
    // ==========================================================================

    /**
     * Fetches visitors linked to a resident.
     *
     * @param residentId resident primary key
     * @return visitor list for the resident
     */
    @GetMapping("/resident/{residentId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY') or @ownershipAuthorization.isCurrentResident(#p0, authentication)")
    public ResponseEntity<ApiResponse<List<VisitorResponseDTO>>> getVisitorsByResident(
            @Positive(message = "Resident ID must be positive")
            @PathVariable Integer residentId
    ) {
        List<VisitorResponseDTO> response =
                visitorService.getVisitorsByResident(residentId);

        return ResponseEntity.ok(
                success("Resident visitors fetched successfully", response)
        );
    }

    // ==========================================================================
    // 8. Get Visitors By Status - GET /api/visitors/status/{status}
    // ==========================================================================

    /**
     * Fetches visitors by workflow status.
     *
     * @param status visitor status
     * @return visitors matching the requested status
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECURITY', 'SECRETARY')")
    public ResponseEntity<ApiResponse<List<VisitorResponseDTO>>> getVisitorsByStatus(
            @PathVariable VisitorStatus status
    ) {
        List<VisitorResponseDTO> response =
                visitorService.getVisitorsByStatus(status);

        return ResponseEntity.ok(
                success("Visitors fetched by status successfully", response)
        );
    }

    // ==========================================================================
    // 9. Get Visitors By Visit Date - GET /api/visitors/visit-date/{visitDate}
    // ==========================================================================

    /**
     * Fetches visitors scheduled for a specific visit date.
     *
     * <p>Date format must be ISO format: {@code yyyy-MM-dd}.
     *
     * @param visitDate visit date
     * @return visitors scheduled for the given date
     */
    @GetMapping("/visit-date/{visitDate}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECURITY', 'SECRETARY')")
    public ResponseEntity<ApiResponse<List<VisitorResponseDTO>>> getVisitorsByVisitDate(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate visitDate
    ) {
        List<VisitorResponseDTO> response =
                visitorService.getVisitorsByVisitDate(visitDate);

        return ResponseEntity.ok(
                success("Visitors fetched by visit date successfully", response)
        );
    }

    // ==========================================================================
    // 10. Delete Visitor - DELETE /api/visitors/{visitorId}
    // ==========================================================================

    /**
     * Deletes a visitor record.
     *
     * <p>Visitors currently checked in should not be deleted. The service layer
     * enforces that workflow rule.
     *
     * @param visitorId visitor primary key
     * @return success response with no data
     */
    @DeleteMapping("/{visitorId}")
    public ResponseEntity<ApiResponse<Void>> deleteVisitor(
            @Positive(message = "Visitor ID must be positive")
            @PathVariable Integer visitorId
    ) {
        visitorService.deleteVisitor(visitorId);

        return ResponseEntity.ok(
                success("Visitor deleted successfully", null)
        );
    }

    // ==========================================================================
    // Private Helpers
    // ==========================================================================

    /**
     * Builds a standard successful API response.
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
