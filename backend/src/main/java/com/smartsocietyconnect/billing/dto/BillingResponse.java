package com.smartsocietyconnect.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.smartsocietyconnect.billing.enums.BillingStatus;

import lombok.*;

/**
 * Response DTO returned after billing operations.
 *
 * <p>This DTO hides internal JPA relationships and exposes only client-friendly
 * billing data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingResponse {

    // ==========================================================================
    // 1. Billing Identity
    // ==========================================================================

    /**
     * UUID billing ID.
     */
    private String billingId;

    // ==========================================================================
    // 2. Resident and Flat Details
    // ==========================================================================

    /**
     * Resident ID linked to this bill.
     */
    private Integer residentId;

    /**
     * Resident full name.
     */
    private String residentName;

    /**
     * Flat number linked to the resident.
     */
    private String flatNumber;

    // ==========================================================================
    // 3. Billing Period
    // ==========================================================================

    /**
     * Billing month from 1 to 12.
     */
    private Integer billingMonth;

    /**
     * Billing year.
     */
    private Integer billingYear;

    // ==========================================================================
    // 4. Charge Components
    // ==========================================================================

    private BigDecimal maintenanceAmount;

    private BigDecimal waterCharge;

    private BigDecimal electricityCharge;

    private BigDecimal parkingCharge;

    private BigDecimal penalty;

    private BigDecimal otherCharge;

    /**
     * Final payable amount.
     */
    private BigDecimal totalAmount;

    // ==========================================================================
    // 5. Due Date and Status
    // ==========================================================================

    private LocalDate dueDate;

    private BillingStatus status;

    // ==========================================================================
    // 6. Additional Information
    // ==========================================================================

    private String remarks;

    // ==========================================================================
    // 7. Audit Fields
    // ==========================================================================

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}