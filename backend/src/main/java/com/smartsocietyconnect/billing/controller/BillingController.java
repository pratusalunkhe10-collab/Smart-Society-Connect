package com.smartsocietyconnect.billing.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.smartsocietyconnect.billing.dto.BillingRequest;
import com.smartsocietyconnect.billing.dto.BillingResponse;
import com.smartsocietyconnect.billing.enums.BillingStatus;
import com.smartsocietyconnect.billing.service.BillingService;
import com.smartsocietyconnect.common.payload.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for Billing Management APIs.
 *
 * <p>This controller exposes endpoints for:
 * <ul>
 *     <li>Generating monthly bills</li>
 *     <li>Updating bill details</li>
 *     <li>Deleting unpaid bills</li>
 *     <li>Fetching bills by ID, resident, and status</li>
 *     <li>Marking pending bills as overdue</li>
 * </ul>
 *
 * <p>All business rules are handled in {@link BillingService}.
 * This controller is responsible only for request validation, routing,
 * and HTTP response formatting.
 */
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Billing Management",
        description = "APIs for managing resident billing records"
)
public class BillingController {

    private final BillingService billingService;

    // ==========================================================================
    // 1. Create Bill - POST /api/billing
    // ==========================================================================

    /**
     * Generate a new monthly bill.
     *
     * @param request billing creation request
     * @return created bill wrapped in ApiResponse
     */
    @PostMapping
    @Operation(
            summary = "Generate Bill",
            description = "Generates a new monthly bill for a resident."
    )
    public ResponseEntity<ApiResponse<BillingResponse>> createBill(
            @Valid @RequestBody BillingRequest request
    ) {

        BillingResponse response =
                billingService.createBill(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(success(
                        "Bill generated successfully",
                        response
                ));
    }

    // ==========================================================================
    // 2. Update Bill - PUT /api/billing/{billingId}
    // ==========================================================================

    /**
     * Update an existing bill.
     *
     * <p>Paid, partially paid, or cancelled bills should not be editable.
     * That rule is enforced in the service layer.
     *
     * @param billingId billing UUID
     * @param request updated billing request
     * @return updated bill wrapped in ApiResponse
     */
    @PutMapping("/{billingId}")
    @Operation(
            summary = "Update Bill",
            description = "Updates an existing billing record."
    )
    public ResponseEntity<ApiResponse<BillingResponse>> updateBill(
            @PathVariable
            @NotBlank(message = "Billing ID is required")
            String billingId,

            @Valid @RequestBody BillingRequest request
    ) {

        BillingResponse response =
                billingService.updateBill(billingId, request);

        return ResponseEntity.ok(
                success(
                        "Bill updated successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 3. Delete Bill - DELETE /api/billing/{billingId}
    // ==========================================================================

    /**
     * Delete a billing record.
     *
     * <p>Paid and partially paid bills should not be deleted.
     * That rule is enforced in the service layer.
     *
     * @param billingId billing UUID
     * @return success response with null data
     */
    @DeleteMapping("/{billingId}")
    @Operation(
            summary = "Delete Bill",
            description = "Deletes a billing record if deletion is allowed."
    )
    public ResponseEntity<ApiResponse<Void>> deleteBill(
            @PathVariable
            @NotBlank(message = "Billing ID is required")
            String billingId
    ) {

        billingService.deleteBill(billingId);

        return ResponseEntity.ok(
                success(
                        "Bill deleted successfully",
                        null
                )
        );
    }

    // ==========================================================================
    // 4. Get Bill By ID - GET /api/billing/{billingId}
    // ==========================================================================

    /**
     * Get bill details by billing ID.
     *
     * @param billingId billing UUID
     * @return bill details wrapped in ApiResponse
     */
    @GetMapping("/{billingId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ACCOUNTANT', 'SECRETARY') or @ownershipAuthorization.ownsBilling(#p0, authentication)")
    @Operation(
            summary = "Get Bill By ID",
            description = "Returns billing details using billing ID."
    )
    public ResponseEntity<ApiResponse<BillingResponse>> getBillById(
            @PathVariable
            @NotBlank(message = "Billing ID is required")
            String billingId
    ) {

        BillingResponse response =
                billingService.getBillById(billingId);

        return ResponseEntity.ok(
                success(
                        "Bill fetched successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 5. Get All Bills - GET /api/billing
    // ==========================================================================

    /**
     * Get all billing records.
     *
     * @return list of bills wrapped in ApiResponse
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ACCOUNTANT', 'SECRETARY')")
    @Operation(
            summary = "Get All Bills",
            description = "Returns all billing records ordered by latest billing period."
    )
    public ResponseEntity<ApiResponse<List<BillingResponse>>> getAllBills() {

        List<BillingResponse> response =
                billingService.getAllBills();

        return ResponseEntity.ok(
                success(
                        "Bills fetched successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 6. Get Bills By Resident - GET /api/billing/resident/{residentId}
    // ==========================================================================

    /**
     * Get all bills for a resident.
     *
     * @param residentId resident primary key
     * @return resident bill list wrapped in ApiResponse
     */
    @GetMapping("/resident/{residentId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY', 'ACCOUNTANT') or @ownershipAuthorization.isCurrentResident(#p0, authentication)")
    @Operation(
            summary = "Get Bills By Resident",
            description = "Returns all bills generated for a resident."
    )
    public ResponseEntity<ApiResponse<List<BillingResponse>>> getBillsByResident(
            @PathVariable
            @Positive(message = "Resident ID must be positive")
            Integer residentId
    ) {

        List<BillingResponse> response =
                billingService.getBillsByResident(residentId);

        return ResponseEntity.ok(
                success(
                        "Resident bills fetched successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 7. Get Bills By Status - GET /api/billing/status/{status}
    // ==========================================================================

    /**
     * Get bills filtered by billing status.
     *
     * <p>Allowed values:
     * {@code PENDING}, {@code PARTIALLY_PAID}, {@code PAID},
     * {@code OVERDUE}, {@code CANCELLED}.
     *
     * @param status billing status
     * @return filtered bill list wrapped in ApiResponse
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ACCOUNTANT', 'SECRETARY')")
    @Operation(
            summary = "Get Bills By Status",
            description = "Returns billing records filtered by status."
    )
    public ResponseEntity<ApiResponse<List<BillingResponse>>> getBillsByStatus(
            @PathVariable BillingStatus status
    ) {

        List<BillingResponse> response =
                billingService.getBillsByStatus(status);

        return ResponseEntity.ok(
                success(
                        "Bills fetched by status successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 8. Mark Bill As Overdue - PATCH /api/billing/{billingId}/overdue
    // ==========================================================================

    /**
     * Mark a pending bill as overdue.
     *
     * <p>The service layer validates whether the due date has already passed.
     *
     * @param billingId billing UUID
     * @return updated bill wrapped in ApiResponse
     */
    @PatchMapping("/{billingId}/overdue")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ACCOUNTANT', 'SECRETARY')")
    @Operation(
            summary = "Mark Bill As Overdue",
            description = "Marks a pending bill as overdue if the due date has expired."
    )
    public ResponseEntity<ApiResponse<BillingResponse>> markAsOverdue(
            @PathVariable
            @NotBlank(message = "Billing ID is required")
            String billingId
    ) {

        BillingResponse response =
                billingService.markAsOverdue(billingId);

        return ResponseEntity.ok(
                success(
                        "Bill marked as overdue successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 9. Success Response Builder
    // ==========================================================================

    /**
     * Build a standard successful API response.
     *
     * <p>Keeps response format consistent with Resident, Visitor, Complaint,
     * and Payment APIs.
     *
     * @param message response message
     * @param data response data
     * @param <T> response data type
     * @return standardized ApiResponse
     */
    private <T> ApiResponse<T> success(String message, T data) {

        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
}
