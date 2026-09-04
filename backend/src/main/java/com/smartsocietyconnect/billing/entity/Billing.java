package com.smartsocietyconnect.billing.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.smartsocietyconnect.billing.enums.BillingStatus;
import com.smartsocietyconnect.resident.entity.Resident;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

/**
 * Entity representing a monthly bill generated for a resident.
 *
 * <p>Maps to the {@code billing} table.
 *
 * <p>Each record stores all charges for one resident for a specific
 * billing month and year.
 *
 * <p><b>Unique bill rule:</b>
 * The combination of {@code resident_id}, {@code billing_month}, and
 * {@code billing_year} must be unique. This prevents duplicate bills
 * for the same resident and billing period.
 *
 * <p><b>Amount handling:</b>
 * All billing amounts use {@link BigDecimal} to avoid floating-point
 * precision errors.
 *
 * <p><b>Timestamp handling:</b>
 * {@link CreationTimestamp} and {@link UpdateTimestamp} are used so Hibernate
 * manages audit fields automatically.
 */
@Entity
@Table(
        name = "billing",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_resident_month_year",
                        columnNames = {
                                "resident_id",
                                "billing_month",
                                "billing_year"
                        }
                )
        },
        indexes = {
                @Index(name = "idx_billing_resident", columnList = "resident_id"),
                @Index(name = "idx_billing_status", columnList = "status"),
                @Index(name = "idx_billing_month_year", columnList = "billing_month, billing_year"),
                @Index(name = "idx_billing_due_date", columnList = "due_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Billing {

    // ==========================================================================
    // 1. Primary Key
    // ==========================================================================

    /**
     * Unique billing identifier.
     *
     * <p>Generated as UUID before first insert.
     */
	@Id
	@Column(
	        name = "billing_id",
	        columnDefinition = "CHAR(36)",
	        nullable = false,
	        updatable = false
	)
	private String billingId;

    // ==========================================================================
    // 2. Resident Relationship
    // ==========================================================================

    /**
     * Resident for whom this bill is generated.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resident_id", nullable = false)
    private Resident resident;

    // ==========================================================================
    // 3. Billing Period
    // ==========================================================================

    /**
     * Billing month from 1 to 12.
     */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Min(value = 1, message = "Billing month must be between 1 and 12")
    @Max(value = 12, message = "Billing month must be between 1 and 12")
    @Column(
            name = "billing_month",
            nullable = false,
            columnDefinition = "TINYINT UNSIGNED"
    )
    private Integer billingMonth;

    /**
     * Billing year.
     *
     * <p>Example: {@code 2026}
     */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Min(value = 2000, message = "Billing year must be valid")
    @Column(
            name = "billing_year",
            nullable = false,
            columnDefinition = "SMALLINT UNSIGNED"
    )
    private Integer billingYear;

    // ==========================================================================
    // 4. Charge Components
    // ==========================================================================

    /**
     * Base monthly maintenance amount.
     */
    @DecimalMin(value = "0.00", message = "Maintenance amount cannot be negative")
    @Column(name = "maintenance_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal maintenanceAmount;

    /**
     * Water charge for the billing period.
     */
    @Builder.Default
    @DecimalMin(value = "0.00", message = "Water charge cannot be negative")
    @Column(name = "water_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal waterCharge = BigDecimal.ZERO;

    /**
     * Electricity charge for the billing period.
     */
    @Builder.Default
    @DecimalMin(value = "0.00", message = "Electricity charge cannot be negative")
    @Column(name = "electricity_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal electricityCharge = BigDecimal.ZERO;

    /**
     * Parking charge for the billing period.
     */
    @Builder.Default
    @DecimalMin(value = "0.00", message = "Parking charge cannot be negative")
    @Column(name = "parking_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal parkingCharge = BigDecimal.ZERO;

    /**
     * Penalty amount, if any.
     */
    @Builder.Default
    @DecimalMin(value = "0.00", message = "Penalty cannot be negative")
    @Column(name = "penalty", nullable = false, precision = 10, scale = 2)
    private BigDecimal penalty = BigDecimal.ZERO;

    /**
     * Any additional manual charge.
     */
    @Builder.Default
    @DecimalMin(value = "0.00", message = "Other charge cannot be negative")
    @Column(name = "other_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal otherCharge = BigDecimal.ZERO;

    /**
     * Final payable amount.
     *
     * <p>This should be calculated in the service layer.
     */
    @DecimalMin(value = "0.00", message = "Total amount cannot be negative")
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // ==========================================================================
    // 5. Due Date and Status
    // ==========================================================================

    /**
     * Last date to pay this bill.
     */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /**
     * Current bill status.
     *
     * <p>Defaults to {@code PENDING}.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BillingStatus status = BillingStatus.PENDING;

    // ==========================================================================
    // 6. Additional Information
    // ==========================================================================

    /**
     * Optional admin remarks.
     */
    @Column(name = "remarks", length = 255)
    private String remarks;

    // ==========================================================================
    // 7. Audit Fields
    // ==========================================================================

    /**
     * Timestamp when the bill was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the bill was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ==========================================================================
    // 8. Entity Lifecycle Hook
    // ==========================================================================

    /**
     * Prepare billing entity before first insert.
     *
     * <p>Only non-timestamp defaults are handled here because timestamps are
     * managed by Hibernate annotations.
     */
    @PrePersist
    public void prePersist() {

        if (billingId == null || billingId.isBlank()) {
            billingId = UUID.randomUUID().toString();
        }

        if (status == null) {
            status = BillingStatus.PENDING;
        }
    }
}