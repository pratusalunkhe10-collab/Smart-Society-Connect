package com.smartsocietyconnect.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

import com.smartsocietyconnect.billing.enums.PaymentMode;
import com.smartsocietyconnect.billing.enums.PaymentStatus;

import lombok.*;

/**
 * Response DTO returned after payment operations.
 *
 * <p>This DTO exposes payment transaction details without exposing the full
 * {@code Billing} entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    // ==========================================================================
    // 1. Payment Identity
    // ==========================================================================

    /**
     * UUID payment ID.
     */
    private String paymentId;

    /**
     * Billing ID against which the payment was made.
     */
    private String billingId;

    // ==========================================================================
    // 2. Payment Details
    // ==========================================================================

    private BigDecimal amountPaid;

    private PaymentMode paymentMode;

    private String transactionReference;

    private String bankName;

    private LocalDate chequeDate;

    private PaymentStatus paymentStatus;

    private LocalDateTime paymentDate;

    private String remarks;

    private Integer recordedByUserId;

    private String recordedByName;
}
