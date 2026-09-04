package com.smartsocietyconnect.visitor.exception;

/**
 * Exception thrown when duplicate visitor data violates a business rule.
 */
public class DuplicateVisitorException extends VisitorException {

    public DuplicateVisitorException(String idProofNumber) {
        super("Visitor already exists with ID proof number: " + idProofNumber);
    }
}