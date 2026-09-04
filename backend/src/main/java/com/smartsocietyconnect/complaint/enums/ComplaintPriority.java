package com.smartsocietyconnect.complaint.enums;

/**
 * Represents the urgency level of a complaint.
 *
 * <p>Maps to the {@code complaints.priority} column:
 * {@code ENUM('LOW','MEDIUM','HIGH','URGENT')}.
 *
 * <p>Default database value should be {@code MEDIUM}.
 */
public enum ComplaintPriority {

    /**
     * Low priority issue that can be handled later.
     */
    LOW,

    /**
     * Normal priority issue.
     */
    MEDIUM,

    /**
     * High priority issue requiring quicker attention.
     */
    HIGH,

    /**
     * Critical issue requiring immediate attention.
     */
    URGENT
}