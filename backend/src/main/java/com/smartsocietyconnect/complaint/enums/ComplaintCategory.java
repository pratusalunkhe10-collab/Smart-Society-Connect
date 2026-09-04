package com.smartsocietyconnect.complaint.enums;

/**
 * Represents the category/type of complaint.
 *
 * <p>Maps to the {@code complaints.category} column:
 * {@code ENUM('PLUMBING','ELECTRICAL','LIFT','PARKING','SECURITY',
 * 'HOUSEKEEPING','WATER','GARDEN','OTHER')}.
 *
 * <p>Used to route complaints to the correct staff/admin team.
 */
public enum ComplaintCategory {

    /**
     * Plumbing-related complaints such as leakage, drainage, or pipe issues.
     */
    PLUMBING,

    /**
     * Electrical complaints such as power issues, wiring, or lights.
     */
    ELECTRICAL,

    /**
     * Lift/elevator-related complaints.
     */
    LIFT,

    /**
     * Parking-related complaints.
     */
    PARKING,

    /**
     * Security-related complaints.
     */
    SECURITY,

    /**
     * Housekeeping or cleanliness-related complaints.
     */
    HOUSEKEEPING,

    /**
     * Water supply or water quality complaints.
     */
    WATER,

    /**
     * Garden, landscaping, or common-area greenery complaints.
     */
    GARDEN,

    /**
     * Any complaint that does not fit predefined categories.
     */
    OTHER
}