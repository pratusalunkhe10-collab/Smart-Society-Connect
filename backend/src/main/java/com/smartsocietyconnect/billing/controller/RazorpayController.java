package com.smartsocietyconnect.billing.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartsocietyconnect.billing.dto.PaymentResponse;
import com.smartsocietyconnect.billing.dto.RazorpayOrderResponse;
import com.smartsocietyconnect.billing.dto.RazorpayVerificationRequest;
import com.smartsocietyconnect.billing.service.RazorpayPaymentService;
import com.smartsocietyconnect.common.payload.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/razorpay")
@RequiredArgsConstructor
@Validated
public class RazorpayController {

    private final RazorpayPaymentService razorpayPaymentService;

    @PostMapping("/orders/{billingId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ACCOUNTANT', 'SECRETARY') or "
            + "@ownershipAuthorization.ownsBilling(#billingId, authentication)")
    public ResponseEntity<ApiResponse<RazorpayOrderResponse>> createOrder(
            @PathVariable @NotBlank String billingId
    ) {
        return ResponseEntity.ok(success(
                "Razorpay order created successfully",
                razorpayPaymentService.createOrder(billingId)
        ));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ACCOUNTANT', 'SECRETARY') or "
            + "@ownershipAuthorization.ownsGatewayOrder(#request.razorpayOrderId, authentication)")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @Valid @RequestBody RazorpayVerificationRequest request
    ) {
        return ResponseEntity.ok(success(
                "Razorpay payment verified successfully",
                razorpayPaymentService.verifyPayment(request)
        ));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature
    ) {
        razorpayPaymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    private <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
}
