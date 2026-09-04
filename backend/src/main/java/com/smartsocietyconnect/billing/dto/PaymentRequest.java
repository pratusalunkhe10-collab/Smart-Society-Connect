package com.smartsocietyconnect.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.smartsocietyconnect.billing.enums.PaymentMode;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for recording a payment against a bill.
 *
 * <p>The service layer should:
 * <ul>
 *     <li>Validate that the billing record exists.</li>
 *     <li>Validate that the payment amount is not more than remaining balance.</li>
 *     <li>Update the bill status after successful payment.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    // ==========================================================================
    // 1. Billing Reference
    // ==========================================================================

    /**
     * Billing ID against which payment is made.
     */
    @NotBlank(message = "Billing ID is required")
    private String billingId;

    // ==========================================================================
    // 2. Payment Details
    // ==========================================================================

    /**
     * Amount paid in this transaction.
     */
    @NotNull(message = "Amount paid is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amountPaid;

    /**
     * Mode used for payment.
     */
    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    /**
     * Optional transaction reference.
     *
     * <p>Required in practice for UPI, CARD, BANK_TRANSFER, and CHEQUE.
     * Usually optional for CASH.
     */
    @Size(max = 100, message = "Transaction reference must not exceed 100 characters")
    private String transactionReference;

    @Size(max = 100, message = "Bank name must not exceed 100 characters")
    private String bankName;

    private LocalDate chequeDate;

    /**
     * Optional payment remarks.
     */
    @Size(max = 255, message = "Remarks must not exceed 255 characters")
    private String remarks;
}
