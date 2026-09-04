package com.smartsocietyconnect.visitor.exception;

/**
 * Exception thrown when a visitor record cannot be found.
 */
public class VisitorNotFoundException extends VisitorException {

    public VisitorNotFoundException(Integer visitorId) {
        super("Visitor not found with id: " + visitorId);
    }
}