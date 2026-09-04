package com.smartsocietyconnect.visitor.enums;

/**
 * Represents supported identity proof types for visitor verification.
 *
 * <p>Maps to the {@code visitors.id_proof_type} column:
 * {@code ENUM('AADHAAR','PAN','PASSPORT','DRIVING_LICENSE','VOTER_ID','OTHER')}.
 *
 * <p>This enum is used to record which document type was provided by the
 * visitor for identity verification.
 */
public enum IdProofType {

    /**
     * Aadhaar card.
     */
    AADHAAR,

    /**
     * Permanent Account Number card.
     */
    PAN,

    /**
     * Passport.
     */
    PASSPORT,

    /**
     * Driving license.
     */
    DRIVING_LICENSE,

    /**
     * Voter identity card.
     */
    VOTER_ID,

    /**
     * Any other valid identity proof accepted by the society.
     */
    OTHER
}