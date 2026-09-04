package com.smartsocietyconnect.billing.dto;

import com.smartsocietyconnect.billing.enums.PaymentStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentStatusUpdateRequest {
    @NotNull(message = "Payment status is required")
    private PaymentStatus status;

    @Size(max = 255, message = "Remarks must not exceed 255 characters")
    private String remarks;
}
