package com.smartsocietyconnect.visitor.enums;

/**
 * Represents the lifecycle status of a visitor entry.
 *
 * <p>Maps to the {@code visitors.status} column:
 * {@code ENUM('REQUESTED','APPROVED','REJECTED','CHECKED_IN','CHECKED_OUT')}.
 *
 * <p><b>Status lifecycle:</b>
 * <pre>
 * REQUESTED   -> APPROVED
 * REQUESTED   -> REJECTED
 * APPROVED    -> CHECKED_IN
 * CHECKED_IN  -> CHECKED_OUT
 * </pre>
 *
 * <p>Use this enum to control visitor workflow transitions in the service layer.
 */
public enum VisitorStatus {

    /**
     * Visitor request has been created but not yet approved or rejected.
     */
    REQUESTED,

    /**
     * Visitor request has been approved by an authorized user.
     */
    APPROVED,

    /**
     * Visitor request has been rejected.
     */
    REJECTED,

    /**
     * Visitor has physically entered the society premises.
     */
    CHECKED_IN,

    /**
     * Visitor has left the society premises.
     */
    CHECKED_OUT
}