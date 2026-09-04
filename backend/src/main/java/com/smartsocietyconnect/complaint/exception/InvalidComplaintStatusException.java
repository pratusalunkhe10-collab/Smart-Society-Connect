package com.smartsocietyconnect.complaint.exception;

import java.io.Serial;

/**
 * Exception thrown when an invalid complaint status transition is attempted.
 *
 * <p>Example: trying to move a CLOSED complaint back to OPEN.
 */
public class InvalidComplaintStatusException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidComplaintStatusException(String message) {
        super(message);
    }
}