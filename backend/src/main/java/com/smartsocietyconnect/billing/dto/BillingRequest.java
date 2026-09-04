package com.smartsocietyconnect.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for creating a billing record.
 *
 * <p>This DTO is used when an admin generates a monthly bill for a resident.
 *
 * <p>The client provides individual charge components. The service layer should
 * calculate {@code totalAmount}; the client should not send it directly.
 *
 * <p>Example total calculation:
 * <pre>
 * totalAmount =
 *     maintenanceAmount
 *     + waterCharge
 *     + electricityCharge
 *     + parkingCharge
 *     + penalty
 *     + otherCharge
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingRequest {

    // ==========================================================================
    // 1. Resident and Billing Period
    // ==========================================================================

    /**
     * Resident ID for whom the bill is generated.
     */
    @NotNull(message = "Resident ID is required")
    private Integer residentId;

    /**
     * Billing month from 1 to 12.
     */
    @NotNull(message = "Billing month is required")
    @Min(value = 1, message = "Billing month must be between 1 and 12")
    @Max(value = 12, message = "Billing month must be between 1 and 12")
    private Integer billingMonth;

    /**
     * Billing year.
     *
     * <p>Example: {@code 2026}
     */
    @NotNull(message = "Billing year is required")
    @Min(value = 2000, message = "Billing year must be valid")
    private Integer billingYear;

    // ==========================================================================
    // 2. Charge Components
    // ==========================================================================

    /**
     * Base maintenance amount.
     */
    @NotNull(message = "Maintenance amount is required")
    @DecimalMin(value = "0.00", message = "Maintenance amount cannot be negative")
    private BigDecimal maintenanceAmount;

    /**
     * Water charge. Defaults to zero when not provided.
     */
    @Builder.Default
    @DecimalMin(value = "0.00", message = "Water charge cannot be negative")
    private BigDecimal waterCharge = BigDecimal.ZERO;

    /**
     * Electricity charge. Defaults to zero when not provided.
     */
    @Builder.Default
    @DecimalMin(value = "0.00", message = "Electricity charge cannot be negative")
    private BigDecimal electricityCharge = BigDecimal.ZERO;

    /**
     * Parking charge. Defaults to zero when not provided.
     */
    @Builder.Default
    @DecimalMin(value = "0.00", message = "Parking charge cannot be negative")
    private BigDecimal parkingCharge = BigDecimal.ZERO;

    /**
     * Penalty amount. Defaults to zero when not provided.
     */
    @Builder.Default
    @DecimalMin(value = "0.00", message = "Penalty cannot be negative")
    private BigDecimal penalty = BigDecimal.ZERO;

    /**
     * Other additional charge. Defaults to zero when not provided.
     */
    @Builder.Default
    @DecimalMin(value = "0.00", message = "Other charge cannot be negative")
    private BigDecimal otherCharge = BigDecimal.ZERO;

    // ==========================================================================
    // 3. Due Date and Remarks
    // ==========================================================================

    /**
     * Due date for paying this bill.
     */
    @NotNull(message = "Due date is required")
    @FutureOrPresent(message = "Due date cannot be in the past")
    private LocalDate dueDate;

    /**
     * Optional billing remarks.
     */
    @Size(max = 255, message = "Remarks must not exceed 255 characters")
    private String remarks;
}