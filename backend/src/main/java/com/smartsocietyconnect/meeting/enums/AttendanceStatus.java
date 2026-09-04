package com.smartsocietyconnect.meeting.enums;

/**
 * Represents a resident's RSVP response for a meeting.
 *
 * <p>Used by the {@code meeting_attendees.attendance_status} column.
 */
public enum AttendanceStatus {

    /**
     * Resident has confirmed attendance.
     */
    GOING,

    /**
     * Resident has declined attendance.
     */
    NOT_GOING,

    /**
     * Resident may attend.
     */
    MAYBE,

    /**
     * Resident has not yet responded.
     */
    PENDING
}