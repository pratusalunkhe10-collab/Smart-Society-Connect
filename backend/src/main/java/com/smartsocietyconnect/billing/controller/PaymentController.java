package com.smartsocietyconnect.billing.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.smartsocietyconnect.billing.dto.PaymentRequest;
import com.smartsocietyconnect.billing.dto.PaymentResponse;
import com.smartsocietyconnect.billing.dto.PaymentStatusUpdateRequest;
import com.smartsocietyconnect.billing.service.PaymentService;
import com.smartsocietyconnect.common.payload.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for Payment Management APIs.
 *
 * <p>This controller exposes endpoints for:
 * <ul>
 *     <li>Recording a payment against a bill</li>
 *     <li>Fetching payment by payment ID</li>
 *     <li>Fetching payment history for a bill</li>
 *     <li>Fetching all payment records</li>
 * </ul>
 *
 * <p>All payment business rules are handled in {@link PaymentService}.
 * This controller only handles request validation, routing, and response
 * formatting.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Payment Management",
        description = "APIs for managing billing payments"
)
public class PaymentController {

    private final PaymentService paymentService;

    // ==========================================================================
    // 1. Make Payment - POST /api/payments
    // ==========================================================================

    /**
     * Record a payment against a bill.
     *
     * <p>The service layer validates the bill, remaining amount,
     * duplicate transaction reference, and bill status.
     *
     * @param request payment request
     * @return saved payment wrapped in ApiResponse
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ACCOUNTANT', 'SECRETARY')")
    @Operation(
            summary = "Record Payment",
            description = "Records a payment against a billing record."
    )
    public ResponseEntity<ApiResponse<PaymentResponse>> makePayment(
            @Valid @RequestBody PaymentRequest request
    ) {

        PaymentResponse response =
                paymentService.makePayment(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(success(
                        "Payment recorded successfully",
                        response
                ));
    }

    @PutMapping("/{paymentId}/status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ACCOUNTANT', 'SECRETARY')")
    @Operation(summary = "Update Offline Payment Status", description = "Marks a pending cheque as cleared or bounced.")
    public ResponseEntity<ApiResponse<PaymentResponse>> updatePaymentStatus(
            @PathVariable @NotBlank String paymentId,
            @Valid @RequestBody PaymentStatusUpdateRequest request) {
        return ResponseEntity.ok(success(
                "Payment status updated successfully",
                paymentService.updatePaymentStatus(paymentId, request)
        ));
    }

    // ==========================================================================
    // 2. Get Payment By ID - GET /api/payments/{paymentId}
    // ==========================================================================

    /**
     * Get payment details by payment ID.
     *
     * @param paymentId payment UUID
     * @return payment details wrapped in ApiResponse
     */
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ACCOUNTANT', 'SECRETARY') or @ownershipAuthorization.ownsPayment(#p0, authentication)")
    @Operation(
            summary = "Get Payment By ID",
            description = "Returns payment details using payment ID."
    )
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable
            @NotBlank(message = "Payment ID is required")
            String paymentId
    ) {

        PaymentResponse response =
                paymentService.getPaymentById(paymentId);

        return ResponseEntity.ok(
                success(
                        "Payment fetched successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 3. Get Payments By Billing ID - GET /api/payments/billing/{billingId}
    // ==========================================================================

    /**
     * Get payment history for a bill.
     *
     * @param billingId billing UUID
     * @return payment history list wrapped in ApiResponse
     */
    @GetMapping("/billing/{billingId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ACCOUNTANT', 'SECRETARY') or @ownershipAuthorization.ownsBilling(#p0, authentication)")
    @Operation(
            summary = "Get Payments By Billing ID",
            description = "Returns payment history for a billing record."
    )
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByBilling(
            @PathVariable
            @NotBlank(message = "Billing ID is required")
            String billingId
    ) {

        List<PaymentResponse> response =
                paymentService.getPaymentsByBilling(billingId);

        return ResponseEntity.ok(
                success(
                        "Payment history fetched successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 4. Get All Payments - GET /api/payments
    // ==========================================================================

    /**
     * Get all payment records.
     *
     * @return payment list wrapped in ApiResponse
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ACCOUNTANT', 'SECRETARY')")
    @Operation(
            summary = "Get All Payments",
            description = "Returns all payment records ordered by latest first."
    )
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getAllPayments() {

        List<PaymentResponse> response =
                paymentService.getAllPayments();

        return ResponseEntity.ok(
                success(
                        "Payments fetched successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 5. Success Response Builder
    // ==========================================================================

    /**
     * Build a standard successful API response.
     *
     * <p>Keeps response format consistent with Billing, Visitor,
     * Complaint, and Resident APIs.
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
