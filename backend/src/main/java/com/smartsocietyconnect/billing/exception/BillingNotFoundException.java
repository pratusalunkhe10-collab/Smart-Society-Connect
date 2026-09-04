package com.smartsocietyconnect.billing.exception;

import java.io.Serial;

/**
 * Exception thrown when a billing record is not found.
 *
 * <p>Usually converted to HTTP {@code 404 Not Found}
 * by {@code GlobalExceptionHandler}.
 */
public class BillingNotFoundException extends BillingException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BillingNotFoundException(String billingId) {
        super("Billing record not found with ID: " + billingId);
    }
}