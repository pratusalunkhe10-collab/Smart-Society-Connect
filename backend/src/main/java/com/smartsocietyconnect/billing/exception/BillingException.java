package com.smartsocietyconnect.billing.exception;

import java.io.Serial;

/**
 * Base unchecked exception for all Billing module errors.
 *
 * <p>Used for general billing business rule failures where a more specific
 * exception is not available.
 *
 * <p>Examples:
 * <ul>
 *     <li>Invalid billing operation</li>
 *     <li>Unable to calculate billing amount</li>
 *     <li>Invalid billing request state</li>
 * </ul>
 */
public class BillingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BillingException(String message) {
        super(message);
    }

    public BillingException(String message, Throwable cause) {
        super(message, cause);
    }
}