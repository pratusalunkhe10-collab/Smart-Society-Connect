package com.smartsocietyconnect.visitor.exception;

import java.io.Serial;

/**
 * Base unchecked exception for Visitor module business errors.
 *
 * <p>All visitor-specific exceptions should extend this class so the global
 * exception handler can handle visitor-module failures consistently.
 */
public class VisitorException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public VisitorException(String message) {
        super(message);
    }

    public VisitorException(String message, Throwable cause) {
        super(message, cause);
    }
}