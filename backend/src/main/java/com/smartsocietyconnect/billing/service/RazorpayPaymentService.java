package com.smartsocietyconnect.billing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartsocietyconnect.billing.dto.PaymentRequest;
import com.smartsocietyconnect.billing.dto.PaymentResponse;
import com.smartsocietyconnect.billing.dto.RazorpayOrderResponse;
import com.smartsocietyconnect.billing.dto.RazorpayVerificationRequest;
import com.smartsocietyconnect.billing.entity.Billing;
import com.smartsocietyconnect.billing.entity.PaymentGatewayOrder;
import com.smartsocietyconnect.billing.entity.PaymentHistory;
import com.smartsocietyconnect.billing.enums.BillingStatus;
import com.smartsocietyconnect.billing.enums.GatewayOrderStatus;
import com.smartsocietyconnect.billing.enums.PaymentMode;
import com.smartsocietyconnect.billing.enums.PaymentStatus;
import com.smartsocietyconnect.billing.exception.BillingNotFoundException;
import com.smartsocietyconnect.billing.exception.PaymentException;
import com.smartsocietyconnect.billing.repository.BillingRepository;
import com.smartsocietyconnect.billing.repository.PaymentGatewayOrderRepository;
import com.smartsocietyconnect.billing.repository.PaymentHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RazorpayPaymentService {

    private static final String RAZORPAY_API = "https://api.razorpay.com/v1";

    private final BillingRepository billingRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final PaymentGatewayOrderRepository gatewayOrderRepository;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @Value("${app.razorpay.key-id:}")
    private String keyId;

    @Value("${app.razorpay.key-secret:}")
    private String keySecret;

    @Value("${app.razorpay.webhook-secret:}")
    private String webhookSecret;

    public RazorpayOrderResponse createOrder(String billingId) {
        requireCheckoutConfiguration();

        Billing billing = billingRepository.findByBillingId(billingId)
                .orElseThrow(() -> new BillingNotFoundException(billingId));
        validateBillCanAcceptPayment(billing);

        BigDecimal remaining = calculateRemainingAmount(billing);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("This bill has no outstanding balance.");
        }

        PaymentGatewayOrder reusable = gatewayOrderRepository
                .findFirstByBillingAndStatusOrderByCreatedAtDesc(billing, GatewayOrderStatus.CREATED)
                .filter(order -> order.getAmount().compareTo(remaining) == 0)
                .orElse(null);
        if (reusable != null) {
            PaymentResponse recoveredPayment = recoverCapturedPayment(reusable);
            if (recoveredPayment != null) {
                return toCompletedCheckoutResponse(reusable, recoveredPayment);
            }
            return toCheckoutResponse(reusable);
        }

        long amountInPaise = toPaise(remaining);
        Map<String, Object> request = Map.of(
                "amount", amountInPaise,
                "currency", "INR",
                "receipt", billing.getBillingId(),
                "notes", Map.of(
                        "billingId", billing.getBillingId(),
                        "residentId", String.valueOf(billing.getResident().getResidentId())
                )
        );

        try {
            JsonNode response = razorpayClient()
                    .post()
                    .uri("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || response.path("id").asText().isBlank()) {
                throw new PaymentException("Razorpay did not return a valid order.");
            }

            PaymentGatewayOrder order = PaymentGatewayOrder.builder()
                    .billing(billing)
                    .razorpayOrderId(response.path("id").asText())
                    .amount(remaining)
                    .currency(response.path("currency").asText("INR"))
                    .status(GatewayOrderStatus.CREATED)
                    .build();

            return toCheckoutResponse(gatewayOrderRepository.save(order));
        } catch (RestClientResponseException exception) {
            log.error("Razorpay order creation failed: status={}, body={}",
                    exception.getStatusCode(), exception.getResponseBodyAsString());
            throw new PaymentException("Unable to start Razorpay payment. Please try again.");
        }
    }

    public PaymentResponse verifyPayment(RazorpayVerificationRequest request) {
        requireCheckoutConfiguration();

        PaymentGatewayOrder order = gatewayOrderRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new PaymentException("Razorpay order was not created by this application."));

        String signedPayload = order.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        if (!isValidSignature(signedPayload, request.getRazorpaySignature(), keySecret)) {
            throw new PaymentException("Razorpay payment signature verification failed.");
        }

        if (order.getStatus() == GatewayOrderStatus.PAID) {
            return findCompletedPayment(order);
        }

        try {
            JsonNode payment = razorpayClient()
                    .get()
                    .uri("/payments/{paymentId}", request.getRazorpayPaymentId())
                    .retrieve()
                    .body(JsonNode.class);

            if (payment == null
                    || !"captured".equalsIgnoreCase(payment.path("status").asText())
                    || !order.getRazorpayOrderId().equals(payment.path("order_id").asText())) {
                throw new PaymentException(
                        "Payment is not captured yet. Enable automatic capture in Razorpay and try again."
                );
            }

            return completePayment(
                    order,
                    request.getRazorpayPaymentId(),
                    payment.path("amount").asLong(),
                    payment.path("method").asText()
            );
        } catch (RestClientResponseException exception) {
            log.error("Razorpay payment verification failed: status={}, body={}",
                    exception.getStatusCode(), exception.getResponseBodyAsString());
            throw new PaymentException("Unable to confirm the Razorpay payment.");
        }
    }

    public void handleWebhook(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new PaymentException("Razorpay webhook secret is not configured.");
        }
        if (!isValidSignature(payload, signature, webhookSecret)) {
            throw new PaymentException("Invalid Razorpay webhook signature.");
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText();
            JsonNode payment = root.path("payload").path("payment").path("entity");
            String orderId = payment.path("order_id").asText();

            if (orderId.isBlank()) {
                return;
            }

            PaymentGatewayOrder order = gatewayOrderRepository
                    .findByRazorpayOrderId(orderId)
                    .orElse(null);
            if (order == null) {
                log.warn("Ignoring Razorpay webhook for unknown order {}", orderId);
                return;
            }

            if ("payment.captured".equals(event)) {
                completePayment(
                        order,
                        payment.path("id").asText(),
                        payment.path("amount").asLong(),
                        payment.path("method").asText()
                );
            } else if ("payment.failed".equals(event)
                    && order.getStatus() == GatewayOrderStatus.CREATED) {
                order.setStatus(GatewayOrderStatus.FAILED);
                gatewayOrderRepository.save(order);
            }
        } catch (PaymentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PaymentException("Unable to process Razorpay webhook.", exception);
        }
    }

    private synchronized PaymentResponse completePayment(
            PaymentGatewayOrder order,
            String paymentId,
            long amountInPaise,
            String razorpayMethod
    ) {
        if (order.getStatus() == GatewayOrderStatus.PAID) {
            return findCompletedPayment(order);
        }
        if (paymentId == null || paymentId.isBlank()) {
            throw new PaymentException("Razorpay payment ID is missing.");
        }
        PaymentHistory existingPayment = paymentHistoryRepository
                .findByTransactionReference(paymentId)
                .orElse(null);
        if (existingPayment != null) {
            order.setRazorpayPaymentId(paymentId);
            order.setStatus(GatewayOrderStatus.PAID);
            gatewayOrderRepository.save(order);
            return findCompletedPayment(order);
        }
        if (toPaise(order.getAmount()) != amountInPaise) {
            throw new PaymentException("Razorpay payment amount does not match the bill amount.");
        }

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .billingId(order.getBilling().getBillingId())
                .amountPaid(order.getAmount())
                .paymentMode(mapPaymentMode(razorpayMethod))
                .transactionReference(paymentId)
                .remarks("Razorpay order " + order.getRazorpayOrderId())
                .build();

        PaymentResponse payment = paymentService.recordVerifiedGatewayPayment(paymentRequest);
        order.setRazorpayPaymentId(paymentId);
        order.setStatus(GatewayOrderStatus.PAID);
        gatewayOrderRepository.save(order);
        return payment;
    }

    private PaymentResponse findCompletedPayment(PaymentGatewayOrder order) {
        String paymentId = order.getRazorpayPaymentId();
        if (paymentId == null || paymentId.isBlank()) {
            throw new PaymentException("Completed Razorpay order is missing its payment reference.");
        }
        PaymentHistory payment = paymentHistoryRepository
                .findByTransactionReference(paymentId)
                .orElseThrow(() -> new PaymentException("Razorpay payment receipt was not found."));
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .billingId(payment.getBilling().getBillingId())
                .amountPaid(payment.getAmountPaid())
                .paymentMode(payment.getPaymentMode())
                .transactionReference(payment.getTransactionReference())
                .paymentStatus(payment.getPaymentStatus())
                .paymentDate(payment.getPaymentDate())
                .remarks(payment.getRemarks())
                .build();
    }

    private RazorpayOrderResponse toCheckoutResponse(PaymentGatewayOrder order) {
        var user = order.getBilling().getResident().getUser();
        String fullName = (user.getFirstName() + " " + user.getLastName()).trim();

        return RazorpayOrderResponse.builder()
                .keyId(keyId)
                .orderId(order.getRazorpayOrderId())
                .amount(toPaise(order.getAmount()))
                .currency(order.getCurrency())
                .name("Smart Society Connect")
                .description("Maintenance bill " + order.getBilling().getBillingId())
                .prefill(Map.of(
                        "name", fullName,
                        "email", user.getEmail(),
                        "contact", user.getMobile() == null ? "" : user.getMobile()
                ))
                .paymentAlreadyCompleted(false)
                .build();
    }

    private RazorpayOrderResponse toCompletedCheckoutResponse(
            PaymentGatewayOrder order,
            PaymentResponse payment
    ) {
        return RazorpayOrderResponse.builder()
                .keyId(keyId)
                .orderId(order.getRazorpayOrderId())
                .amount(toPaise(order.getAmount()))
                .currency(order.getCurrency())
                .name("Smart Society Connect")
                .description("Maintenance bill " + order.getBilling().getBillingId())
                .paymentAlreadyCompleted(true)
                .completedPayment(payment)
                .build();
    }

    private PaymentResponse recoverCapturedPayment(PaymentGatewayOrder order) {
        try {
            JsonNode response = razorpayClient()
                    .get()
                    .uri("/orders/{orderId}/payments", order.getRazorpayOrderId())
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                return null;
            }
            for (JsonNode payment : response.path("items")) {
                if ("captured".equalsIgnoreCase(payment.path("status").asText())
                        && order.getRazorpayOrderId().equals(payment.path("order_id").asText())) {
                    return completePayment(
                            order,
                            payment.path("id").asText(),
                            payment.path("amount").asLong(),
                            payment.path("method").asText()
                    );
                }
            }
            return null;
        } catch (RestClientResponseException exception) {
            log.error("Unable to reconcile Razorpay order {}: status={}, body={}",
                    order.getRazorpayOrderId(), exception.getStatusCode(),
                    exception.getResponseBodyAsString());
            throw new PaymentException("Unable to check the previous Razorpay payment. Please try again.");
        }
    }

    private BigDecimal calculateRemainingAmount(Billing billing) {
        BigDecimal paid = paymentHistoryRepository.findByBilling(billing).stream()
                .filter(payment -> payment.getPaymentStatus() == PaymentStatus.SUCCESS)
                .map(PaymentHistory::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return billing.getTotalAmount().subtract(paid);
    }

    private void validateBillCanAcceptPayment(Billing billing) {
        if (billing.getStatus() == BillingStatus.CANCELLED) {
            throw new PaymentException("Payment cannot be made for a cancelled bill.");
        }
        if (billing.getStatus() == BillingStatus.PAID) {
            throw new PaymentException("This bill has already been paid.");
        }
    }

    private PaymentMode mapPaymentMode(String method) {
        return switch (String.valueOf(method).toLowerCase()) {
            case "upi" -> PaymentMode.UPI;
            case "card" -> PaymentMode.CARD;
            case "netbanking", "bank_transfer" -> PaymentMode.BANK_TRANSFER;
            default -> throw new PaymentException(
                    "Unsupported Razorpay payment method: " + method
            );
        };
    }

    private long toPaise(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    private RestClient razorpayClient() {
        return RestClient.builder()
                .baseUrl(RAZORPAY_API)
                .defaultHeaders(headers -> headers.setBasicAuth(keyId, keySecret))
                .build();
    }

    private boolean isValidSignature(String payload, String receivedSignature, String secret) {
        if (receivedSignature == null || receivedSignature.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String generated = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(
                    generated.getBytes(StandardCharsets.US_ASCII),
                    receivedSignature.getBytes(StandardCharsets.US_ASCII)
            );
        } catch (Exception exception) {
            throw new PaymentException("Unable to verify Razorpay signature.", exception);
        }
    }

    private void requireCheckoutConfiguration() {
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            throw new PaymentException(
                    "Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET."
            );
        }
    }
}
