package com.smartsocietyconnect.billing.service;

import java.util.List;

import com.smartsocietyconnect.billing.dto.PaymentRequest;
import com.smartsocietyconnect.billing.dto.PaymentResponse;
import com.smartsocietyconnect.billing.dto.PaymentStatusUpdateRequest;

/**
 * Service contract for Payment module operations.
 *
 * <p>This interface defines payment-related business operations for bills.
 *
 * <p>Implementation responsibilities include:
 * <ul>
 *     <li>Validating that the bill exists.</li>
 *     <li>Preventing payments against cancelled bills.</li>
 *     <li>Preventing duplicate transaction references.</li>
 *     <li>Ensuring payment amount does not exceed remaining balance.</li>
 *     <li>Updating bill status after successful payment.</li>
 *     <li>Returning payment history responses.</li>
 * </ul>
 */
public interface PaymentService {

    // ==========================================================================
    // 1. Payment Write Operation
    // ==========================================================================

    /**
     * Record a payment against a bill.
     *
     * @param request payment request
     * @return saved payment response
     */
    PaymentResponse makePayment(PaymentRequest request);

    /**
     * Persist a payment that has already been authenticated and captured by
     * the configured payment gateway. This method is intentionally not
     * exposed by PaymentController.
     */
    PaymentResponse recordVerifiedGatewayPayment(PaymentRequest request);

    PaymentResponse updatePaymentStatus(String paymentId, PaymentStatusUpdateRequest request);

    // ==========================================================================
    // 2. Payment Read Operations
    // ==========================================================================

    /**
     * Get payment by payment ID.
     *
     * @param paymentId payment UUID
     * @return payment response
     */
    PaymentResponse getPaymentById(String paymentId);

    /**
     * Get payment history for a bill.
     *
     * @param billingId billing UUID
     * @return payment records linked to the bill
     */
    List<PaymentResponse> getPaymentsByBilling(String billingId);

    /**
     * Get all payment records.
     *
     * @return all payment records
     */
    List<PaymentResponse> getAllPayments();
}
