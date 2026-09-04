package com.smartsocietyconnect.meeting.exception;

import java.io.Serial;

/**
 * Thrown when an invalid operation is attempted on a completed meeting.
 */
public class MeetingAlreadyCompletedException extends MeetingException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MeetingAlreadyCompletedException(String meetingUuid) {
        super("Meeting is already completed: " + meetingUuid);
    }
}