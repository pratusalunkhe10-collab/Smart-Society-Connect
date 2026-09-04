package com.smartsocietyconnect.billing.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.smartsocietyconnect.billing.enums.PaymentMode;
import com.smartsocietyconnect.billing.enums.PaymentStatus;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

/**
 * Entity representing a payment transaction made against a bill.
 *
 * <p>Maps to the {@code payment_history} table.
 *
 * <p>A single bill can have multiple payment records, which supports
 * partial payments.
 *
 * <p><b>Payment status vs billing status:</b>
 * {@link PaymentStatus} belongs to this individual transaction, while
 * {@link com.smartsocietyconnect.billing.enums.BillingStatus} belongs to
 * the full bill.
 */
@Entity
@Table(
        name = "payment_history",
        indexes = {
                @Index(name = "idx_payment_billing", columnList = "billing_id"),
                @Index(name = "idx_payment_status", columnList = "payment_status"),
                @Index(name = "idx_payment_mode", columnList = "payment_mode"),
                @Index(name = "idx_payment_date", columnList = "payment_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PaymentHistory {

    // ==========================================================================
    // 1. Primary Key
    // ==========================================================================

    /**
     * Unique payment identifier.
     *
     * <p>Generated as UUID before first insert.
     */
	@Id
	@Column(
	        name = "payment_id",
	        columnDefinition = "CHAR(36)",
	        nullable = false,
	        updatable = false
	)
	private String paymentId;

    // ==========================================================================
    // 2. Billing Relationship
    // ==========================================================================

    /**
     * Bill against which this payment was made.
     */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "billing_id", nullable = false)
	private Billing billing;

    // ==========================================================================
    // 3. Payment Amount
    // ==========================================================================

    /**
     * Amount paid in this transaction.
     */
    @DecimalMin(value = "0.01", message = "Amount paid must be greater than zero")
    @Column(name = "amount_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid;

    // ==========================================================================
    // 4. Payment Method and Reference
    // ==========================================================================

    /**
     * Mode used for payment.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false)
    private PaymentMode paymentMode;

    /**
     * Optional unique transaction reference.
     *
     * <p>Examples: UPI reference, cheque number, bank transaction ID.
     */
    @Column(name = "transaction_reference", unique = true, length = 100)
    private String transactionReference;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "cheque_date")
    private LocalDate chequeDate;

    // ==========================================================================
    // 5. Payment Status and Date
    // ==========================================================================

    /**
     * Current status of this payment transaction.
     *
     * <p>Defaults to {@code SUCCESS}.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.SUCCESS;

    /**
     * Date and time when the payment was made.
     *
     * <p>If not provided, current timestamp is used.
     */
    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    // ==========================================================================
    // 6. Additional Information
    // ==========================================================================

    /**
     * Optional payment remarks.
     */
    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "recorded_by_user_id")
    private Integer recordedByUserId;

    @Column(name = "recorded_by_name", length = 100)
    private String recordedByName;

    // ==========================================================================
    // 7. Audit Field
    // ==========================================================================

    /**
     * Timestamp when this payment record was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ==========================================================================
    // 8. Entity Lifecycle Hook
    // ==========================================================================

    /**
     * Prepare payment entity before first insert.
     *
     * <p>Only non-timestamp defaults are handled here because createdAt is
     * managed by Hibernate.
     */
    @PrePersist
    public void prePersist() {

        if (paymentId == null || paymentId.isBlank()) {
            paymentId = UUID.randomUUID().toString();
        }

        if (paymentDate == null) {
            paymentDate = LocalDateTime.now();
        }

        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.SUCCESS;
        }
    }
}
