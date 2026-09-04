package com.smartsocietyconnect.resident.entity;

/**
 * Verification status of an uploaded resident document.
 *
 * <p>Maps to the {@code verification_status} column
 * of the {@code resident_documents} table:
 * <pre>{@code
 * verification_status ENUM(
 *     'PENDING',
 *     'VERIFIED',
 *     'REJECTED'
 * ) DEFAULT 'PENDING'
 * }</pre>
 *
 * <p><b>Persistence strategy: {@code @Enumerated(EnumType.STRING)}</b>
 *
 * <p>All Java constant names match the MySQL ENUM values exactly —
 * no custom {@code AttributeConverter} needed.
 *
 * <p><b>Usage on the {@code ResidentDocument} entity field:</b>
 * <pre>{@code
 * @Enumerated(EnumType.STRING)
 * @Column(
 *     name             = "verification_status",
 *     columnDefinition = "ENUM('PENDING','VERIFIED','REJECTED') DEFAULT 'PENDING'"
 * )
 * @Builder.Default
 * private VerificationStatus verificationStatus = VerificationStatus.PENDING;
 * }</pre>
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see ResidentDocument
 * @see DocumentType
 */
public enum VerificationStatus {

    /**
     * Document has been uploaded but not yet reviewed by admin.
     *
     * <p>This is the default status on upload.
     * Admin must review and change to {@link #VERIFIED} or {@link #REJECTED}.
     */
    PENDING,

    /**
     * Document has been reviewed and approved by admin.
     *
     * <p>Resident's document is considered valid and accepted.
     */
    VERIFIED,

    /**
     * Document has been reviewed and rejected by admin.
     *
     * <p>Resident must re-upload a corrected document.
     * Rejection reason should be communicated separately.
     */
    REJECTED
}