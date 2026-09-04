package com.smartsocietyconnect.visitor.enums;

/**
 * Represents the gender of a visitor.
 *
 * <p>Maps to the {@code visitors.gender} column:
 * {@code ENUM('MALE','FEMALE','OTHER')}.
 *
 * <p>This enum is used during visitor registration and visitor profile
 * response mapping.
 */
public enum Gender {

    /**
     * Male visitor.
     */
    MALE,

    /**
     * Female visitor.
     */
    FEMALE,

    /**
     * Visitor identifies as another gender or prefers not to specify.
     */
    OTHER
}