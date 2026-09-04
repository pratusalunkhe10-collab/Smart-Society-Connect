package com.smartsocietyconnect.complaint.enums;

/**
 * Represents the lifecycle status of a complaint.
 *
 * <p>Maps to the {@code complaints.status} column:
 * {@code ENUM('OPEN','IN_PROGRESS','RESOLVED','CLOSED','REJECTED')}.
 *
 * <p><b>Status lifecycle:</b>
 * <pre>
 * OPEN        -> IN_PROGRESS
 * IN_PROGRESS -> RESOLVED
 * RESOLVED    -> CLOSED
 * OPEN        -> REJECTED
 * </pre>
 */
public enum ComplaintStatus {

    /**
     * Complaint has been submitted and is waiting for action.
     */
    OPEN,

    /**
     * Complaint is currently being handled by staff/admin.
     */
    IN_PROGRESS,

    /**
     * Complaint has been resolved by staff/admin.
     */
    RESOLVED,

    /**
     * Complaint has been confirmed closed after resolution.
     */
    CLOSED,

    /**
     * Complaint was rejected because it is invalid, duplicate, or not actionable.
     */
    REJECTED
}