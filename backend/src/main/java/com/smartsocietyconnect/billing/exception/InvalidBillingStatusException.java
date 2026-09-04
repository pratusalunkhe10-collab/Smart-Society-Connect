package com.smartsocietyconnect.billing.exception;

import java.io.Serial;

import com.smartsocietyconnect.billing.enums.BillingStatus;

/**
 * Exception thrown when an invalid billing status operation is attempted.
 *
 * <p>Examples:
 * <ul>
 *     <li>Trying to record payment for a {@code CANCELLED} bill</li>
 *     <li>Trying to cancel an already {@code PAID} bill</li>
 *     <li>Trying to update a bill that is already closed/paid</li>
 * </ul>
 *
 * <p>Usually converted to HTTP {@code 400 Bad Request}
 * by {@code GlobalExceptionHandler}.
 */
public class InvalidBillingStatusException extends BillingException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidBillingStatusException(String message) {
        super(message);
    }

    public InvalidBillingStatusException(
            BillingStatus currentStatus,
            BillingStatus expectedStatus
    ) {

        super("Invalid billing status. Expected: "
                + expectedStatus
                + ", Current: "
                + currentStatus);
    }
}