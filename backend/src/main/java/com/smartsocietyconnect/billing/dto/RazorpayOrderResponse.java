package com.smartsocietyconnect.billing.dto;

import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RazorpayOrderResponse {

    private String keyId;
    private String orderId;
    private Long amount;
    private String currency;
    private String name;
    private String description;
    private Map<String, String> prefill;
    private boolean paymentAlreadyCompleted;
    private PaymentResponse completedPayment;
}
