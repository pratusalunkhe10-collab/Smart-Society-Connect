package com.smartsocietyconnect.complaint.exception;

import java.io.Serial;

/**
 * Exception thrown for general complaint-module business rule violations.
 *
 * <p>Use this when the failure is not specifically not-found, duplicate,
 * or invalid-status related.
 */
public class ComplaintOperationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ComplaintOperationException(String message) {
        super(message);
    }
}