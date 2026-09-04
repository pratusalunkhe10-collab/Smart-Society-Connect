package com.smartsocietyconnect.resident.entity;

//import jakarta.persistence.EnumType;
//import jakarta.persistence.Enumerated;

/**
 * Enumeration of occupancy statuses for a flat in Smart Society Connect.
 *
 * <p>Maps to the {@code status} column of the {@code flats} table:
 * <pre>{@code
 * status ENUM('VACANT', 'OCCUPIED') DEFAULT 'VACANT'
 * }</pre>
 *
 * <p><b>Persistence strategy: {@code @Enumerated(EnumType.STRING)}</b>
 *
 * <p>Unlike {@link FlatType}, no {@code AttributeConverter} is needed here.
 * The Java constant names ({@code VACANT}, {@code OCCUPIED}) are valid
 * identifiers and match the MySQL ENUM values exactly — there is no
 * underscore-prefix workaround that would leak into the database.
 * {@code EnumType.STRING} stores the name directly and is the correct,
 * simplest solution.
 *
 * <p><b>Usage on the {@code Flat} entity field:</b>
 * <pre>{@code
 * @Builder.Default
 * @Enumerated(EnumType.STRING)
 * @Column(name = "status",
 *         columnDefinition = "ENUM('VACANT','OCCUPIED') DEFAULT 'VACANT'")
 * private FlatStatus status = FlatStatus.VACANT;
 * }</pre>
 *
 * <p><b>Why {@code @Builder.Default} is required:</b>
 * The DB column defaults to {@code 'VACANT'}, but Lombok's {@code @Builder}
 * bypasses field initializers. Without {@code @Builder.Default}, constructing
 * a {@code Flat} via the builder will leave {@code status} as {@code null},
 * silently violating the intended default.
 *
 * <p><b>Why not {@code EnumType.ORDINAL}?</b>
 * Ordinal persistence stores the declaration index (0 for {@code VACANT},
 * 1 for {@code OCCUPIED}). Inserting a new constant or reordering existing
 * ones silently remaps every existing row. {@code EnumType.STRING} is always
 * preferred for readable, stable column values.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see FlatType
 */
public enum FlatStatus {

    /**
     * The flat is unoccupied and available for a new resident.
     *
     * <p>This is the default status assigned to every newly created flat,
     * matching the DB column default: {@code DEFAULT 'VACANT'}.
     */
    VACANT,

    /**
     * The flat is currently occupied by a resident.
     *
     * <p>Set when a {@code Resident} record is successfully linked to this flat.
     */
    OCCUPIED

}