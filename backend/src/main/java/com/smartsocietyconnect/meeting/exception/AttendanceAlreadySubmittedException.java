package com.smartsocietyconnect.meeting.exception;

import java.io.Serial;

/**
 * Thrown when a resident tries to submit a duplicate RSVP for the same meeting.
 */
public class AttendanceAlreadySubmittedException extends MeetingException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AttendanceAlreadySubmittedException(Integer residentId, String meetingUuid) {
        super("Resident ID "
                + residentId
                + " has already submitted RSVP for meeting: "
                + meetingUuid);
    }
}