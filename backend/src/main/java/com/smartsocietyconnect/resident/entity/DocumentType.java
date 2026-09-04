package com.smartsocietyconnect.resident.entity;

/**
 * Enumeration of document types that can be uploaded by residents
 * in Smart Society Connect.
 *
 * <p>Maps to the {@code document_type} column of the {@code documents} table:
 * <pre>{@code
 * document_type ENUM(
 *     'AADHAAR',
 *     'PAN',
 *     'PROOF_OF_ADDRESS',
 *     'OWNERSHIP',
 *     'RENT_AGREEMENT',
 *     'PROFILE_PHOTO',
 *     'OTHER'
 * ) NOT NULL
 * }</pre>
 *
 * <p><b>Persistence strategy: {@code @Enumerated(EnumType.STRING)}</b>
 *
 * <p>No {@code AttributeConverter} is needed. All Java constant names
 * ({@code AADHAAR}, {@code PAN}, etc.) are valid identifiers and match
 * the MySQL ENUM values exactly — there is no underscore-prefix workaround
 * that would leak into the database. {@code EnumType.STRING} stores the
 * name directly and is the correct, simplest solution.
 *
 * <p>Compare with {@link FlatType}, which requires a custom
 * {@link FlatType.FlatTypeConverter} because its Java names carry an
 * underscore prefix ({@code _2BHK}) not present in the DB ENUM definition.
 *
 * <p><b>Usage on the {@code Document} entity field:</b>
 * <pre>{@code
 * @Enumerated(EnumType.STRING)
 * @Column(name = "document_type", nullable = false,
 *         columnDefinition = "ENUM('AADHAAR','PAN','PROOF_OF_ADDRESS'," +
 *                            "'OWNERSHIP','RENT_AGREEMENT','PROFILE_PHOTO','OTHER')")
 * private DocumentType documentType;
 * }</pre>
 *
 * <p><b>Why not {@code EnumType.ORDINAL}?</b>
 * Ordinal persistence stores the declaration index (0 for {@code AADHAAR},
 * 1 for {@code PAN}, etc.). Inserting a new constant or reordering existing
 * ones silently remaps every existing row in the database.
 * {@code EnumType.STRING} is always preferred for readable, stable values.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see RelationType
 * @see ResidentType
 * @see FlatType
 */
public enum DocumentType {

    // ==========================================================================
    // Government Identity Documents
    // ==========================================================================

    /**
     * Aadhaar card — 12-digit unique identity number issued by UIDAI.
     *
     * <p>Commonly used as proof of identity and address for Indian residents.
     * Required for KYC verification during resident registration.
     */
    AADHAAR,

    /**
     * PAN card — Permanent Account Number issued by the Income Tax Department.
     *
     * <p>Used as proof of identity and for financial/tax-related verification.
     */
    PAN,

    /**
     * Any government-issued proof of address document other than Aadhaar
     * (e.g. passport, voter ID, utility bill, bank statement).
     */
    PROOF_OF_ADDRESS,

    // ==========================================================================
    // Property Documents
    // ==========================================================================

    /**
     * Ownership document proving the resident legally owns the flat
     * (e.g. sale deed, property registration certificate).
     *
     * <p>Applicable only for residents with {@link ResidentType#OWNER}.
     */
    OWNERSHIP,

    /**
     * Rent or leave-and-licence agreement between the owner and tenant.
     *
     * <p>Applicable only for residents with {@link ResidentType#TENANT}.
     * Should be uploaded when a tenant is registered to validate the
     * tenancy and its duration.
     */
    RENT_AGREEMENT,

    // ==========================================================================
    // Profile
    // ==========================================================================

    /**
     * Resident's profile photograph.
     *
     * <p>Used for identification and display within the society portal.
     * Recommended format: JPEG or PNG, max 2 MB.
     */
    PROFILE_PHOTO,

    // ==========================================================================
    // Catch-all
    // ==========================================================================

    /**
     * Any document that does not fall into the predefined categories above.
     *
     * <p>When this value is stored, consider capturing the actual document
     * name or description in the {@code document_name} field of the
     * {@code Document} entity for display and audit purposes.
     */
    OTHER

}