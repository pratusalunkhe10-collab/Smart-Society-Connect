package com.smartsocietyconnect.billing.exception;

import java.io.Serial;

/**
 * Base unchecked exception for all payment-related errors.
 *
 * <p>Used for general payment business rule failures where a more specific
 * exception is not available.
 *
 * <p>Examples:
 * <ul>
 *     <li>Payment amount exceeds remaining bill balance</li>
 *     <li>Duplicate transaction reference</li>
 *     <li>Payment not allowed for cancelled bill</li>
 * </ul>
 */
public class PaymentException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}