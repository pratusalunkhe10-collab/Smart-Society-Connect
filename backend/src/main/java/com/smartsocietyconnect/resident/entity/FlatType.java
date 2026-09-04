package com.smartsocietyconnect.resident.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.Getter;

/**
 * Enumeration of flat/unit types available in a Smart Society Connect society.
 *
 * <p>Each constant carries a {@code displayValue}, which is the clean
 * human-readable value used in API requests, API responses, and database storage.
 *
 * <p>Java enum constants cannot start with a number, so constants use a leading
 * underscore internally, for example {@code _2BHK}. The external value remains
 * clean as {@code 2BHK}.
 *
 * <p><b>Important:</b>
 * <ul>
 *     <li>JPA uses {@link FlatTypeConverter} for database conversion.</li>
 *     <li>Jackson uses {@link JsonCreator} and {@link JsonValue} for JSON conversion.</li>
 * </ul>
 *
 * <p>This allows Swagger/Postman/API clients to send:
 * <pre>{@code
 * {
 *   "flatType": "2BHK"
 * }
 * }</pre>
 *
 * instead of:
 * <pre>{@code
 * {
 *   "flatType": "_2BHK"
 * }
 * }</pre>
 */
@Getter
public enum FlatType {

    // ==========================================================================
    // Constants
    // ==========================================================================

    /**
     * Single room with kitchen.
     */
    _1RK("1RK"),

    /**
     * One bedroom, hall, and kitchen.
     */
    _1BHK("1BHK"),

    /**
     * Two bedrooms, hall, and kitchen.
     */
    _2BHK("2BHK"),

    /**
     * Three bedrooms, hall, and kitchen.
     */
    _3BHK("3BHK"),

    /**
     * Four bedrooms, hall, and kitchen.
     */
    _4BHK("4BHK");

    // ==========================================================================
    // Fields
    // ==========================================================================

    /**
     * Clean value used in JSON and database.
     *
     * <p>Examples: {@code 1RK}, {@code 1BHK}, {@code 2BHK}.
     */
    private final String displayValue;

    // ==========================================================================
    // Constructor
    // ==========================================================================

    /**
     * Creates a flat type with its clean external value.
     *
     * @param displayValue value used in JSON and database
     */
    FlatType(String displayValue) {
        this.displayValue = displayValue;
    }

    // ==========================================================================
    // JSON Serialization
    // ==========================================================================

    /**
     * Converts enum to JSON value.
     *
     * <p>Without this, API responses may show {@code _2BHK}.
     * With this, API responses show {@code 2BHK}.
     *
     * @return clean JSON value
     */
    @JsonValue
    public String toJson() {
        return displayValue;
    }

    // ==========================================================================
    // JSON Deserialization And Reverse Lookup
    // ==========================================================================

    /**
     * Converts JSON/database value to enum.
     *
     * <p>Accepts both clean values such as {@code 2BHK} and internal enum names
     * such as {@code _2BHK}. This makes the API friendly while still supporting
     * older/internal clients if needed.
     *
     * @param value incoming JSON or database value
     * @return matching {@link FlatType}
     * @throws IllegalArgumentException if the value is invalid
     */
    @JsonCreator
    public static FlatType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalizedValue = value.trim();

        for (FlatType type : values()) {
            if (type.displayValue.equalsIgnoreCase(normalizedValue)
                    || type.name().equalsIgnoreCase(normalizedValue)) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Invalid flat type: " + value
                        + ". Allowed values are: 1RK, 1BHK, 2BHK, 3BHK, 4BHK"
        );
    }

    /**
     * Backward-compatible method used by JPA converter and existing code.
     *
     * @param value display value from database
     * @return matching {@link FlatType}
     */
    public static FlatType fromDisplayValue(String value) {
        return fromValue(value);
    }

    // ==========================================================================
    // JPA AttributeConverter
    // ==========================================================================

    /**
     * JPA converter for storing {@link FlatType} in MySQL.
     *
     * <p>Stores clean values like {@code 2BHK}, not Java enum names like
     * {@code _2BHK}.
     */
    @Converter(autoApply = false)
    public static class FlatTypeConverter
            implements AttributeConverter<FlatType, String> {

        /**
         * Converts enum to database value.
         *
         * @param flatType enum value
         * @return database value
         */
        @Override
        public String convertToDatabaseColumn(FlatType flatType) {
            return flatType == null ? null : flatType.getDisplayValue();
        }

        /**
         * Converts database value to enum.
         *
         * @param dbValue database value
         * @return enum value
         */
        @Override
        public FlatType convertToEntityAttribute(String dbValue) {
            return dbValue == null ? null : FlatType.fromDisplayValue(dbValue);
        }
    }
}