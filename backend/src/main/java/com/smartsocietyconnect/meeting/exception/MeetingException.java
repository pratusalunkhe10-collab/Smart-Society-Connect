package com.smartsocietyconnect.meeting.exception;

import java.io.Serial;

/**
 * Base unchecked exception for Meeting Module errors.
 *
 * <p>All meeting-specific exceptions should extend this class so the global
 * exception handler can handle module errors consistently.
 */
public class MeetingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MeetingException(String message) {
        super(message);
    }

    public MeetingException(String message, Throwable cause) {
        super(message, cause);
    }
}