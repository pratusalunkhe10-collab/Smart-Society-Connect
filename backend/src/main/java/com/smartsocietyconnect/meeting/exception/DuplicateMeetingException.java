package com.smartsocietyconnect.meeting.exception;

import java.io.Serial;

/**
 * Thrown when attempting to create a duplicate meeting.
 */
public class DuplicateMeetingException extends MeetingException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DuplicateMeetingException(String message) {
        super(message);
    }
}