package com.smartsocietyconnect.meeting.exception;

import java.io.Serial;

/**
 * Thrown when an invalid operation is attempted on a cancelled meeting.
 */
public class MeetingCancelledException extends MeetingException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MeetingCancelledException(String meetingUuid) {
        super("Meeting is cancelled: " + meetingUuid);
    }
}