package com.smartsocietyconnect.billing.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartsocietyconnect.billing.dto.PaymentRequest;
import com.smartsocietyconnect.billing.dto.PaymentResponse;
import com.smartsocietyconnect.billing.dto.PaymentStatusUpdateRequest;
import com.smartsocietyconnect.billing.entity.Billing;
import com.smartsocietyconnect.billing.entity.PaymentHistory;
import com.smartsocietyconnect.billing.enums.BillingStatus;
import com.smartsocietyconnect.billing.enums.PaymentStatus;
import com.smartsocietyconnect.billing.enums.PaymentMode;
import com.smartsocietyconnect.billing.exception.BillingNotFoundException;
import com.smartsocietyconnect.billing.exception.PaymentException;
import com.smartsocietyconnect.billing.exception.PaymentNotFoundException;
import com.smartsocietyconnect.billing.mapper.PaymentHistoryMapper;
import com.smartsocietyconnect.billing.repository.BillingRepository;
import com.smartsocietyconnect.billing.repository.PaymentHistoryRepository;
import com.smartsocietyconnect.billing.service.PaymentService;
import com.smartsocietyconnect.notification.service.NotificationService;
import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for payment operations.
 *
 * <p>This class contains financial transaction logic for the Billing module.
 *
 * <p>Responsibilities:
 * <ul>
 *     <li>Validate billing record before accepting payment.</li>
 *     <li>Prevent payment against cancelled or already paid bills.</li>
 *     <li>Prevent duplicate transaction references.</li>
 *     <li>Calculate remaining bill balance from successful payment history.</li>
 *     <li>Reject overpayment.</li>
 *     <li>Save payment history.</li>
 *     <li>Update bill status automatically after successful payment.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentHistoryRepository paymentHistoryRepository;
    private final BillingRepository billingRepository;
    private final PaymentHistoryMapper paymentHistoryMapper;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // ==========================================================================
    // 1. Make Payment
    // ==========================================================================

    /**
     * Record a payment against a billing record.
     *
     * <p>Business rules:
     * <ul>
     *     <li>Billing record must exist.</li>
     *     <li>Payment is not allowed for {@code CANCELLED} bills.</li>
     *     <li>Payment is not allowed for already {@code PAID} bills.</li>
     *     <li>Transaction reference must be unique if provided.</li>
     *     <li>Payment amount must not exceed remaining bill amount.</li>
     *     <li>Bill status is updated after successful payment.</li>
     * </ul>
     *
     * @param request payment request
     * @return saved payment response
     */
    @Override
    public PaymentResponse makePayment(PaymentRequest request) {
        return recordPayment(request, true);
    }

    @Override
    public PaymentResponse recordVerifiedGatewayPayment(PaymentRequest request) {
        return recordPayment(request, false);
    }

    private PaymentResponse recordPayment(PaymentRequest request, boolean offlinePayment) {

        Billing billing = getBilling(request.getBillingId());

        validateBillCanAcceptPayment(billing);
        if (offlinePayment) {
            validateOfflinePayment(request);
        } else {
            validateVerifiedGatewayPayment(request);
        }
        if (offlinePayment && request.getPaymentMode() == PaymentMode.CASH
                && (request.getTransactionReference() == null || request.getTransactionReference().isBlank())) {
            request.setTransactionReference("CASH-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        }
        validateTransactionReference(request.getTransactionReference());

        BigDecimal remainingAmount =
                calculateRemainingAmount(billing);

        validateOutstandingBalance(remainingAmount);

        validatePaymentAmount(
                request.getAmountPaid(),
                remainingAmount
        );

        PaymentHistory payment =
                paymentHistoryMapper.toEntity(request, billing);

        if (offlinePayment) {
            User recorder = currentRecorder();
            payment.setRecordedByUserId(recorder.getUserId());
            payment.setRecordedByName((recorder.getFirstName() + " "
                    + (recorder.getLastName() == null ? "" : recorder.getLastName())).trim());
        }
        payment.setPaymentStatus(offlinePayment && request.getPaymentMode() == PaymentMode.CHEQUE
                ? PaymentStatus.PENDING : PaymentStatus.SUCCESS);

        PaymentHistory savedPayment =
                paymentHistoryRepository.save(payment);

        if (savedPayment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            updateBillingStatusAfterPayment(billing);
            BigDecimal updatedBalance = calculateRemainingAmount(billing);
            notificationService.notifyUser(billing.getResident().getUser(), "PAYMENT", "Payment received successfully",
                    "₹" + savedPayment.getAmountPaid() + " received via " + savedPayment.getPaymentMode()
                            + ". Remaining balance: ₹" + updatedBalance + ".",
                    "/app/billing");
        } else {
            notificationService.notifyUser(billing.getResident().getUser(), "PAYMENT", "Cheque received for verification",
                    "Cheque " + savedPayment.getTransactionReference() + " for ₹" + savedPayment.getAmountPaid()
                            + " is pending bank clearance.", "/app/billing");
        }

        log.info(
                "Payment recorded successfully. paymentId={}, billingId={}, amount={}",
                savedPayment.getPaymentId(),
                billing.getBillingId(),
                savedPayment.getAmountPaid()
        );

        return paymentHistoryMapper.toResponse(savedPayment);
    }

    @Override
    public PaymentResponse updatePaymentStatus(String paymentId, PaymentStatusUpdateRequest request) {
        PaymentHistory payment = getPayment(paymentId);
        if (payment.getPaymentMode() != PaymentMode.CHEQUE) {
            throw new PaymentException("Only cheque payments require clearance updates.");
        }
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new PaymentException("Only a pending cheque can be cleared or bounced.");
        }
        if (request.getStatus() != PaymentStatus.SUCCESS && request.getStatus() != PaymentStatus.FAILED) {
            throw new PaymentException("Cheque status must be SUCCESS (cleared) or FAILED (bounced).");
        }

        Billing billing = payment.getBilling();
        if (request.getStatus() == PaymentStatus.SUCCESS) {
            validatePaymentAmount(payment.getAmountPaid(), calculateRemainingAmount(billing));
        }
        payment.setPaymentStatus(request.getStatus());
        if (request.getRemarks() != null && !request.getRemarks().isBlank()) {
            payment.setRemarks(request.getRemarks().trim());
        }
        PaymentHistory saved = paymentHistoryRepository.save(payment);

        if (saved.getPaymentStatus() == PaymentStatus.SUCCESS) {
            updateBillingStatusAfterPayment(billing);
            notificationService.notifyUser(billing.getResident().getUser(), "PAYMENT", "Cheque payment cleared",
                    "Cheque " + saved.getTransactionReference() + " was cleared successfully.", "/app/billing");
        } else {
            notificationService.notifyUser(billing.getResident().getUser(), "PAYMENT", "Cheque payment bounced",
                    "Cheque " + saved.getTransactionReference() + " was marked as bounced. Your bill remains unpaid.",
                    "/app/billing");
        }
        return paymentHistoryMapper.toResponse(saved);
    }

    private void validateOfflinePayment(PaymentRequest request) {
        if (request.getPaymentMode() != PaymentMode.CASH && request.getPaymentMode() != PaymentMode.CHEQUE) {
            throw new PaymentException("Manual payment records support CASH or CHEQUE only.");
        }
        if (request.getPaymentMode() == PaymentMode.CHEQUE) {
            if (request.getTransactionReference() == null || request.getTransactionReference().isBlank()) {
                throw new PaymentException("Cheque number is required.");
            }
            if (request.getBankName() == null || request.getBankName().isBlank()) {
                throw new PaymentException("Bank name is required for cheque payments.");
            }
            if (request.getChequeDate() == null) {
                throw new PaymentException("Cheque date is required.");
            }
        }
    }

    private void validateVerifiedGatewayPayment(PaymentRequest request) {
        if (request.getPaymentMode() == PaymentMode.CASH
                || request.getPaymentMode() == PaymentMode.CHEQUE) {
            throw new PaymentException("Gateway payments must use UPI, CARD, or BANK_TRANSFER.");
        }
        if (request.getTransactionReference() == null
                || request.getTransactionReference().isBlank()) {
            throw new PaymentException("Gateway payment reference is required.");
        }
    }

    private User currentRecorder() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new PaymentException("Authenticated payment recorder was not found.");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new PaymentException("Authenticated payment recorder was not found."));
    }

    // ==========================================================================
    // 2. Get Payment By ID
    // ==========================================================================

    /**
     * Get payment details by payment ID.
     *
     * @param paymentId payment UUID
     * @return payment response
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String paymentId) {

        PaymentHistory payment = getPayment(paymentId);

        return paymentHistoryMapper.toResponse(payment);
    }

    // ==========================================================================
    // 3. Get Payments By Billing
    // ==========================================================================

    /**
     * Get all payment records for a billing record.
     *
     * <p>The billing record is validated first so invalid billing IDs return
     * a clear billing-related error.
     *
     * @param billingId billing UUID
     * @return payment history list
     */
    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByBilling(String billingId) {

        Billing billing = getBilling(billingId);

        return paymentHistoryRepository
                .findByBillingOrderByPaymentDateDesc(billing)
                .stream()
                .map(paymentHistoryMapper::toResponse)
                .toList();
    }

    // ==========================================================================
    // 4. Get All Payments
    // ==========================================================================

    /**
     * Get all payment records.
     *
     * <p>Recommended repository method:
     * {@code findAllByOrderByPaymentDateDesc()} so latest payments appear first.
     *
     * @return payment response list
     */
    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {

        return paymentHistoryRepository
                .findAllByOrderByPaymentDateDesc()
                .stream()
                .map(paymentHistoryMapper::toResponse)
                .toList();
    }

    // ==========================================================================
    // 5. Helper - Fetch Billing
    // ==========================================================================

    /**
     * Fetch billing record by billing ID.
     *
     * @param billingId billing UUID
     * @return billing entity
     */
    private Billing getBilling(String billingId) {

        return billingRepository.findByBillingId(billingId)
                .orElseThrow(() ->
                        new BillingNotFoundException(billingId)
                );
    }

    // ==========================================================================
    // 6. Helper - Fetch Payment
    // ==========================================================================

    /**
     * Fetch payment record by payment ID.
     *
     * @param paymentId payment UUID
     * @return payment history entity
     */
    private PaymentHistory getPayment(String paymentId) {

        return paymentHistoryRepository.findByPaymentId(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(paymentId)
                );
    }

    // ==========================================================================
    // 7. Helper - Validate Billing Status
    // ==========================================================================

    /**
     * Validate whether the bill can accept payment.
     *
     * @param billing billing entity
     */
    private void validateBillCanAcceptPayment(Billing billing) {

        if (billing.getStatus() == BillingStatus.CANCELLED) {
            throw new PaymentException(
                    "Payment cannot be made for a cancelled bill."
            );
        }

        if (billing.getStatus() == BillingStatus.PAID) {
            throw new PaymentException(
                    "This bill has already been paid."
            );
        }
    }

    // ==========================================================================
    // 8. Helper - Validate Transaction Reference
    // ==========================================================================

    /**
     * Validate duplicate transaction reference.
     *
     * <p>Blank transaction references are ignored because cash payments may not
     * have a transaction reference.
     *
     * @param transactionReference external transaction reference
     */
    private void validateTransactionReference(String transactionReference) {

        if (transactionReference == null
                || transactionReference.isBlank()) {
            return;
        }

        if (paymentHistoryRepository
                .existsByTransactionReference(transactionReference)) {

            throw new PaymentException(
                    "Transaction reference already exists."
            );
        }
    }

    // ==========================================================================
    // 9. Helper - Calculate Remaining Amount
    // ==========================================================================

    /**
     * Calculate remaining bill balance.
     *
     * <p>The remaining balance is calculated from successful payment history,
     * not stored separately, to avoid duplicate state.
     *
     * @param billing billing entity
     * @return remaining amount
     */
    private BigDecimal calculateRemainingAmount(Billing billing) {

        BigDecimal paidAmount = paymentHistoryRepository
                .findByBilling(billing)
                .stream()
                .filter(payment ->
                        payment.getPaymentStatus() == PaymentStatus.SUCCESS
                )
                .map(PaymentHistory::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return billing.getTotalAmount().subtract(paidAmount);
    }

    // ==========================================================================
    // 10. Helper - Validate Outstanding Balance
    // ==========================================================================

    /**
     * Validate that the bill has a payable outstanding balance.
     *
     * @param remainingAmount remaining bill amount
     */
    private void validateOutstandingBalance(BigDecimal remainingAmount) {

        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException(
                    "No outstanding balance available for payment."
            );
        }
    }

    // ==========================================================================
    // 11. Helper - Validate Payment Amount
    // ==========================================================================

    /**
     * Validate payment amount against remaining bill amount.
     *
     * @param amountPaid requested payment amount
     * @param remainingAmount remaining bill amount
     */
    private void validatePaymentAmount(
            BigDecimal amountPaid,
            BigDecimal remainingAmount
    ) {

        if (amountPaid.compareTo(remainingAmount) > 0) {
            throw new PaymentException(
                    "Payment amount exceeds remaining bill amount."
            );
        }
    }

    // ==========================================================================
    // 12. Helper - Update Billing Status
    // ==========================================================================

    /**
     * Update billing status after successful payment.
     *
     * <p>If remaining amount becomes zero, bill is marked as {@code PAID}.
     * Otherwise, bill is marked as {@code PARTIALLY_PAID}.
     *
     * @param billing billing entity
     */
    private void updateBillingStatusAfterPayment(Billing billing) {

        BigDecimal remaining =
                calculateRemainingAmount(billing);

        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            billing.setStatus(BillingStatus.PAID);
        } else {
            billing.setStatus(BillingStatus.PARTIALLY_PAID);
        }

        billingRepository.save(billing);
    }
}
