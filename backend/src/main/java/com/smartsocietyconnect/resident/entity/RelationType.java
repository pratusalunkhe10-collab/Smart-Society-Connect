package com.smartsocietyconnect.resident.entity;

/**
 * Enumeration of family relationships between a family member and
 * the primary resident in Smart Society Connect.
 *
 * <p>Maps to the {@code relation} column of the {@code family_members} table:
 * <pre>{@code
 * relation ENUM(
 *     'FATHER','MOTHER','SPOUSE',
 *     'SON','DAUGHTER',
 *     'BROTHER','SISTER',
 *     'OTHER'
 * ) NOT NULL
 * }</pre>
 *
 * <p><b>Persistence strategy: {@code @Enumerated(EnumType.STRING)}</b>
 *
 * <p>No {@code AttributeConverter} is needed. All Java constant names
 * ({@code FATHER}, {@code MOTHER}, etc.) are valid identifiers and match
 * the MySQL ENUM values exactly — there is no underscore-prefix workaround
 * that would leak into the database. {@code EnumType.STRING} stores the
 * name directly and is the correct, simplest solution.
 *
 * <p>Compare with {@link FlatType}, which requires a custom
 * {@link FlatType.FlatTypeConverter} because its Java names carry an
 * underscore prefix ({@code _2BHK}) not present in the DB ENUM definition.
 *
 * <p><b>Usage on the {@code FamilyMember} entity field:</b>
 * <pre>{@code
 * @Enumerated(EnumType.STRING)
 * @Column(name = "relation", nullable = false,
 *         columnDefinition = "ENUM('FATHER','MOTHER','SPOUSE'," +
 *                            "'SON','DAUGHTER','BROTHER','SISTER','OTHER')")
 * private RelationType relation;
 * }</pre>
 *
 * <p><b>Why not {@code EnumType.ORDINAL}?</b>
 * Ordinal persistence stores the declaration index (0 for {@code FATHER},
 * 1 for {@code MOTHER}, etc.). Inserting a new constant or reordering
 * existing ones silently remaps every existing row in the database.
 * {@code EnumType.STRING} is always preferred for readable, stable values.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see FlatStatus
 * @see ResidentType
 * @see FlatType
 */
public enum RelationType {

    // ==========================================================================
    // Parental Relations
    // ==========================================================================

    /**
     * The family member is the resident's father.
     */
    FATHER,

    /**
     * The family member is the resident's mother.
     */
    MOTHER,

    // ==========================================================================
    // Spousal Relation
    // ==========================================================================

    /**
     * The family member is the resident's spouse (husband or wife).
     */
    SPOUSE,

    // ==========================================================================
    // Children
    // ==========================================================================

    /**
     * The family member is the resident's son.
     */
    SON,

    /**
     * The family member is the resident's daughter.
     */
    DAUGHTER,

    // ==========================================================================
    // Siblings
    // ==========================================================================

    /**
     * The family member is the resident's brother.
     */
    BROTHER,

    /**
     * The family member is the resident's sister.
     */
    SISTER,

    // ==========================================================================
    // Catch-all
    // ==========================================================================

    /**
     * The family member's relationship does not fall into any of the
     * predefined categories above (e.g. grandparent, uncle, aunt, cousin).
     *
     * <p>When this value is stored, consider capturing the actual relationship
     * in a separate free-text field on the {@code FamilyMember} entity
     * for display and reporting purposes.
     */
    OTHER

}