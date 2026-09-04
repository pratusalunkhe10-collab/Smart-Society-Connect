package com.smartsocietyconnect.billing.exception;

import java.io.Serial;

/**
 * Exception thrown when a duplicate bill is generated.
 *
 * <p>A resident should have only one bill for a specific billing month
 * and billing year.
 *
 * <p>Usually converted to HTTP {@code 409 Conflict}
 * by {@code GlobalExceptionHandler}.
 */
public class DuplicateBillingException extends BillingException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DuplicateBillingException(
            Integer residentId,
            Integer month,
            Integer year
    ) {

        super("Billing already exists for resident ID "
                + residentId
                + " for "
                + month
                + "/"
                + year);
    }
}