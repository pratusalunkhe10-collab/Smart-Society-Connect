package com.smartsocietyconnect.resident.exception;

import java.io.Serial;

/**
 * Custom unchecked exception for Resident Module errors.
 *
 * <p>Used for resident-module business failures such as:
 * <ul>
 *     <li>Resident not found</li>
 *     <li>Flat not found</li>
 *     <li>Family member not found</li>
 *     <li>Document not found</li>
 *     <li>Duplicate resident profile</li>
 *     <li>Duplicate family member relation</li>
 *     <li>Invalid resident operations</li>
 *     <li>File upload or file deletion failures</li>
 * </ul>
 *
 * <p>This exception does not decide the HTTP status code by itself.
 * HTTP response mapping is handled centrally in
 * {@code GlobalExceptionHandler}.
 */
public class ResidentException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    // ==========================================================================
    // Constructors
    // ==========================================================================

    /**
     * Creates a resident exception with a user-readable business message.
     *
     * @param message explanation of the resident-module failure
     */
    public ResidentException(String message) {
        super(message);
    }

    /**
     * Creates a resident exception with a message and root cause.
     *
     * <p>Use this constructor when wrapping lower-level exceptions such as
     * {@code IOException}, file storage failures, or persistence errors.
     *
     * @param message explanation of the resident-module failure
     * @param cause original exception that caused this failure
     */
    public ResidentException(String message, Throwable cause) {
        super(message, cause);
    }
}