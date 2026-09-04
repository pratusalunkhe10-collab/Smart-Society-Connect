package com.smartsocietyconnect.complaint.exception;

import java.io.Serial;

/**
 * Exception thrown when a duplicate complaint number is detected.
 *
 * <p>Complaint numbers must be unique because they are used as public
 * tracking identifiers.
 */
public class DuplicateComplaintException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DuplicateComplaintException(String message) {
        super(message);
    }
}