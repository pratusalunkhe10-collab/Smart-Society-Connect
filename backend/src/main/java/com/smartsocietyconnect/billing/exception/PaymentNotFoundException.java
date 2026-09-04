package com.smartsocietyconnect.billing.exception;

import java.io.Serial;

/**
 * Exception thrown when a payment history record is not found.
 *
 * <p>Usually converted to HTTP {@code 404 Not Found}
 * by {@code GlobalExceptionHandler}.
 */
public class PaymentNotFoundException extends PaymentException {

    @Serial
    private static final long serialVersionUID = 1L;

    public PaymentNotFoundException(String paymentId) {
        super("Payment record not found with ID: " + paymentId);
    }
}