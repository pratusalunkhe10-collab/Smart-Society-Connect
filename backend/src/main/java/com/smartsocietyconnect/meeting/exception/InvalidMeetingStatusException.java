package com.smartsocietyconnect.meeting.exception;

import java.io.Serial;

import com.smartsocietyconnect.meeting.enums.MeetingStatus;

/**
 * Thrown when an invalid meeting status transition is attempted.
 */
public class InvalidMeetingStatusException extends MeetingException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidMeetingStatusException(String message) {
        super(message);
    }

    public InvalidMeetingStatusException(
            MeetingStatus currentStatus,
            MeetingStatus requestedStatus
    ) {
        super("Invalid meeting status transition from "
                + currentStatus
                + " to "
                + requestedStatus);
    }
}