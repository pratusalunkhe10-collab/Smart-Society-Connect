package com.smartsocietyconnect.resident.entity;

/**
 * Enumeration of residency types for a flat occupant in Smart Society Connect.
 *
 * <p>Maps to the {@code resident_type} column of the {@code residents} table:
 * <pre>{@code
 * resident_type ENUM('OWNER', 'TENANT') NOT NULL
 * }</pre>
 *
 * <p><b>Persistence strategy: {@code @Enumerated(EnumType.STRING)}</b>
 *
 * <p>No {@code AttributeConverter} is needed here. The Java constant names
 * ({@code OWNER}, {@code TENANT}) are valid identifiers and match the MySQL
 * ENUM values exactly — there is no underscore-prefix workaround that would
 * leak into the database. {@code EnumType.STRING} stores the name directly
 * and is the correct, simplest solution.
 *
 * <p>Compare with {@link FlatType}, which requires a custom
 * {@link FlatType.FlatTypeConverter} because its Java names carry an
 * underscore prefix ({@code _2BHK}) not present in the DB ENUM definition.
 *
 * <p><b>Usage on the {@code Resident} entity field:</b>
 * <pre>{@code
 * @Enumerated(EnumType.STRING)
 * @Column(name = "resident_type", nullable = false,
 *         columnDefinition = "ENUM('OWNER','TENANT')")
 * private ResidentType residentType;
 * }</pre>
 *
 * <p><b>Why not {@code EnumType.ORDINAL}?</b>
 * Ordinal persistence stores the declaration index (0 for {@code OWNER},
 * 1 for {@code TENANT}). Inserting a new constant or reordering existing
 * ones silently remaps every existing row. {@code EnumType.STRING} is always
 * preferred for readable, stable column values.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see FlatStatus
 * @see FlatType
 */
public enum ResidentType {

    /**
     * The resident owns the flat.
     *
     * <p>Owners may or may not reside in the flat themselves —
     * they may have tenants occupying it on their behalf.
     */
    OWNER,

    /**
     * The resident is renting the flat from an owner.
     *
     * <p>Tenants should have an associated rent agreement document
     * uploaded via the {@code resident_documents} table.
     */
    TENANT

}