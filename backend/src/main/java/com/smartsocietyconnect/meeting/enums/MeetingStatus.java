package com.smartsocietyconnect.meeting.enums;

/**
 * Represents the lifecycle status of a society meeting.
 *
 * <p>Used by the {@code meetings.status} column.
 */
public enum MeetingStatus {

    /**
     * Meeting has been scheduled but has not yet started.
     */
    SCHEDULED,

    /**
     * Meeting is currently in progress.
     */
    ONGOING,

    /**
     * Meeting has been successfully completed.
     */
    COMPLETED,

    /**
     * Meeting has been cancelled.
     */
    CANCELLED
}