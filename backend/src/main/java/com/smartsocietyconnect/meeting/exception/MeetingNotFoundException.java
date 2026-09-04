package com.smartsocietyconnect.meeting.exception;

import java.io.Serial;

/**
 * Thrown when a meeting cannot be found by ID or UUID.
 */
public class MeetingNotFoundException extends MeetingException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MeetingNotFoundException(Integer meetingId) {
        super("Meeting not found with ID: " + meetingId);
    }

    public MeetingNotFoundException(String meetingUuid) {
        super("Meeting not found with UUID: " + meetingUuid);
    }
}