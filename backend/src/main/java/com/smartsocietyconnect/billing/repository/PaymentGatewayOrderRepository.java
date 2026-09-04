package com.smartsocietyconnect.billing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartsocietyconnect.billing.entity.Billing;
import com.smartsocietyconnect.billing.entity.PaymentGatewayOrder;
import com.smartsocietyconnect.billing.enums.GatewayOrderStatus;

@Repository
public interface PaymentGatewayOrderRepository extends JpaRepository<PaymentGatewayOrder, String> {

    Optional<PaymentGatewayOrder> findByRazorpayOrderId(String razorpayOrderId);

    Optional<PaymentGatewayOrder> findFirstByBillingAndStatusOrderByCreatedAtDesc(
            Billing billing,
            GatewayOrderStatus status
    );

    boolean existsByRazorpayOrderIdAndBillingResidentUserEmailIgnoreCase(
            String razorpayOrderId,
            String email
    );
}
