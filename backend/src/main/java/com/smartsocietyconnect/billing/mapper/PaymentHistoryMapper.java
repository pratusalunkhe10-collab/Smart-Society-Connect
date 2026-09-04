package com.smartsocietyconnect.billing.mapper;

import org.springframework.stereotype.Component;

import com.smartsocietyconnect.billing.dto.PaymentRequest;
import com.smartsocietyconnect.billing.dto.PaymentResponse;
import com.smartsocietyconnect.billing.entity.Billing;
import com.smartsocietyconnect.billing.entity.PaymentHistory;

/**
 * Mapper class for converting between Payment DTOs and PaymentHistory entity.
 *
 * <p>This mapper keeps payment-related conversion logic separate from the
 * service layer.
 *
 * <p><b>Important:</b>
 * Payment validation, duplicate transaction checks, balance calculation, and
 * billing status updates should remain in the service layer.
 */
@Component
public class PaymentHistoryMapper {

    // ==========================================================================
    // 1. Convert Request DTO to Entity
    // ==========================================================================

    /**
     * Convert {@link PaymentRequest} to {@link PaymentHistory}.
     *
     * <p>The {@link Billing} entity is passed from the service layer because
     * the request contains only {@code billingId}.
     *
     * @param request payment request from client
     * @param billing billing entity fetched by service layer
     * @return payment history entity
     */
    public PaymentHistory toEntity(
            PaymentRequest request,
            Billing billing
    ) {

        if (request == null) {
            return null;
        }

        return PaymentHistory.builder()
                .billing(billing)
                .amountPaid(request.getAmountPaid())
                .paymentMode(request.getPaymentMode())
                .transactionReference(request.getTransactionReference())
                .bankName(request.getBankName())
                .chequeDate(request.getChequeDate())
                .remarks(request.getRemarks())
                .build();
    }

    // ==========================================================================
    // 2. Convert Entity to Response DTO
    // ==========================================================================

    /**
     * Convert {@link PaymentHistory} entity to {@link PaymentResponse}.
     *
     * @param paymentHistory payment history entity
     * @return payment response DTO
     */
    public PaymentResponse toResponse(
            PaymentHistory paymentHistory
    ) {

        if (paymentHistory == null) {
            return null;
        }

        Billing billing = paymentHistory.getBilling();

        return PaymentResponse.builder()
                .paymentId(paymentHistory.getPaymentId())
                .billingId(resolveBillingId(billing))
                .amountPaid(paymentHistory.getAmountPaid())
                .paymentMode(paymentHistory.getPaymentMode())
                .transactionReference(paymentHistory.getTransactionReference())
                .bankName(paymentHistory.getBankName())
                .chequeDate(paymentHistory.getChequeDate())
                .paymentStatus(paymentHistory.getPaymentStatus())
                .paymentDate(paymentHistory.getPaymentDate())
                .remarks(paymentHistory.getRemarks())
                .recordedByUserId(paymentHistory.getRecordedByUserId())
                .recordedByName(paymentHistory.getRecordedByName())
                .build();
    }

    // ==========================================================================
    // 3. Private Helper Methods
    // ==========================================================================

    /**
     * Resolve billing ID safely.
     */
    private String resolveBillingId(Billing billing) {

        if (billing == null) {
            return null;
        }

        return billing.getBillingId();
    }
}
