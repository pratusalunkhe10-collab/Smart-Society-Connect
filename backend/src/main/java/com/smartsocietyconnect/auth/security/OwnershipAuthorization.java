package com.smartsocietyconnect.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.smartsocietyconnect.auth.repository.UserRepository;
import com.smartsocietyconnect.billing.repository.BillingRepository;
import com.smartsocietyconnect.billing.repository.PaymentGatewayOrderRepository;
import com.smartsocietyconnect.billing.repository.PaymentHistoryRepository;
import com.smartsocietyconnect.complaint.repository.ComplaintRepository;
import com.smartsocietyconnect.resident.repository.ResidentRepository;
import com.smartsocietyconnect.resident.repository.ResidentDocumentRepository;
import com.smartsocietyconnect.visitor.repository.VisitorRepository;

import lombok.RequiredArgsConstructor;

/**
 * Authorization helper used by controller method-security expressions.
 *
 * <p>It verifies that an ID supplied in a resident-facing route belongs to
 * the authenticated account. Staff roles are granted separately in the
 * controller expressions.</p>
 */
@Component("ownershipAuthorization")
@RequiredArgsConstructor
public class OwnershipAuthorization {

    private final UserRepository userRepository;
    private final ResidentRepository residentRepository;
    private final ResidentDocumentRepository residentDocumentRepository;
    private final VisitorRepository visitorRepository;
    private final ComplaintRepository complaintRepository;
    private final BillingRepository billingRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final PaymentGatewayOrderRepository paymentGatewayOrderRepository;

    public boolean isCurrentUser(Integer userId, Authentication authentication) {
        return authentication != null
                && userRepository.findById(userId)
                        .map(user -> user.getEmail().equalsIgnoreCase(authentication.getName()))
                        .orElse(false);
    }

    public boolean isCurrentResident(Integer residentId, Authentication authentication) {
        return authentication != null
                && residentRepository.findById(residentId)
                        .map(resident -> resident.getUser().getEmail()
                                .equalsIgnoreCase(authentication.getName()))
                        .orElse(false);
    }

    public boolean ownsVisitor(Integer visitorId, Authentication authentication) {
        return hasAuthenticatedEmail(authentication)
                && visitorRepository.existsByVisitorIdAndResidentUserEmailIgnoreCase(
                        visitorId, authentication.getName());
    }

    public boolean ownsComplaint(Integer complaintId, Authentication authentication) {
        return hasAuthenticatedEmail(authentication)
                && complaintRepository.existsByComplaintIdAndResidentUserEmailIgnoreCase(
                        complaintId, authentication.getName());
    }

    public boolean ownsBilling(String billingId, Authentication authentication) {
        return hasAuthenticatedEmail(authentication)
                && billingRepository.existsByBillingIdAndResidentUserEmailIgnoreCase(
                        billingId, authentication.getName());
    }

    public boolean ownsDocument(Integer residentDocId, Authentication authentication) {
        return hasAuthenticatedEmail(authentication)
                && residentDocumentRepository
                        .existsByResidentDocIdAndResidentUserEmailIgnoreCase(
                                residentDocId, authentication.getName());
    }

    public boolean ownsPayment(String paymentId, Authentication authentication) {
        return hasAuthenticatedEmail(authentication)
                && paymentHistoryRepository
                        .existsByPaymentIdAndBillingResidentUserEmailIgnoreCase(
                                paymentId, authentication.getName());
    }

    public boolean ownsGatewayOrder(String razorpayOrderId, Authentication authentication) {
        return hasAuthenticatedEmail(authentication)
                && paymentGatewayOrderRepository
                        .existsByRazorpayOrderIdAndBillingResidentUserEmailIgnoreCase(
                                razorpayOrderId, authentication.getName());
    }

    private boolean hasAuthenticatedEmail(Authentication authentication) {
        return authentication != null && authentication.getName() != null;
    }
}
