package com.smartsocietyconnect.complaint.exception;

import java.io.Serial;

/**
 * Exception thrown when a complaint cannot be found.
 *
 * <p>Use this for lookups by complaint ID or complaint number.
 */
public class ComplaintNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ComplaintNotFoundException(String message) {
        super(message);
    }
}