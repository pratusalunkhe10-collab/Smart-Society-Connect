package com.smartsocietyconnect.visitor.exception;

import com.smartsocietyconnect.visitor.enums.VisitorStatus;

/**
 * Exception thrown when a visitor workflow transition is not allowed.
 */
public class InvalidVisitorStatusException extends VisitorException {

    public InvalidVisitorStatusException(
            VisitorStatus currentStatus,
            VisitorStatus expectedStatus
    ) {
        super("Invalid visitor status. Expected: "
                + expectedStatus
                + ", Current: "
                + currentStatus);
    }
}