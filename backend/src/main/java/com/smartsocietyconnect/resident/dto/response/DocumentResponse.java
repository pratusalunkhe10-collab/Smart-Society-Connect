package com.smartsocietyconnect.resident.dto.response;

import java.time.LocalDateTime;

import com.smartsocietyconnect.resident.entity.DocumentType;
import com.smartsocietyconnect.resident.entity.VerificationStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO returned after document upload and verification operations.
 *
 * <p>Represents a flattened, read-only snapshot combining data from
 * two tables — {@code documents} and {@code resident_documents} —
 * into a single response object for the client.
 *
 * <ul>
 *     <li>{@code documents} — file metadata (name, type, path, size, MIME)</li>
 *     <li>{@code resident_documents} — ownership and verification workflow
 *         (who uploaded, who verified, current status)</li>
 * </ul>
 *
 * <p>This is an outbound-only DTO — never deserialized from client
 * requests. {@code @Builder} is sufficient; no {@code @NoArgsConstructor}
 * or {@code @AllArgsConstructor} is needed since Jackson only
 * serializes (writes) this object to JSON.
 *
 * <p>Field types match their source entity fields exactly to avoid
 * silent type conversion errors during mapping.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see com.smartsocietyconnect.resident.entity.Document
 * @see com.smartsocietyconnect.resident.entity.ResidentDocument
 * @see DocumentType
 * @see VerificationStatus
 */
@Getter
@Builder
public class DocumentResponse {

    // =========================================================================
    // resident_documents fields
    // =========================================================================

    /**
     * Unique identifier of the resident-document mapping record.
     *
     * <p>Maps from {@code resident_documents.resident_doc_id}
     * (INT UNSIGNED → Integer).
     */
    private Integer residentDocId;

    /**
     * ID of the resident who owns this document.
     *
     * <p>Maps from {@code resident_documents.resident_id}
     * (INT UNSIGNED → Integer).
     */
    private Integer residentId;

    /** Full name of the resident who owns this document, for staff search. */
    private String residentName;

    /**
     * ID of the underlying document metadata record.
     *
     * <p>Maps from {@code resident_documents.document_id}
     * (INT UNSIGNED → Integer).
     */
    private Integer documentId;

    // =========================================================================
    // documents fields
    // =========================================================================

    /**
     * Display name of the document.
     *
     * <p>Maps from {@code documents.document_name}
     * (VARCHAR(100) → String).
     * Example: {@code "Aadhaar Card"}, {@code "Rent Agreement 2024"}
     */
    private String documentName;

    /**
     * Category/type of the document.
     *
     * <p>Maps from {@code documents.document_type} (ENUM → DocumentType).
     * One of: {@code AADHAAR}, {@code PAN}, {@code PROOF_OF_ADDRESS},
     * {@code OWNERSHIP}, {@code RENT_AGREEMENT},
     * {@code PROFILE_PHOTO}, {@code OTHER}.
     */
    private DocumentType documentType;

    /**
     * MIME type of the uploaded file.
     *
     * <p>Maps from {@code documents.mime_type} (VARCHAR(50) → String).
     * Examples: {@code "application/pdf"}, {@code "image/jpeg"}
     * Nullable — may not always be recorded.
     */
    private String mimeType;

    /**
     * Path or storage key of the uploaded file.
     *
     * <p>Maps from {@code documents.file_path} (VARCHAR(255) → String).
     * May be a relative path on disk or an object-storage key (e.g. S3).
     * Never exposes binary file content directly.
     */
    private String filePath;

    /**
     * Size of the uploaded file in bytes.
     *
     * <p>Maps from {@code documents.file_size}
     * (MEDIUMINT UNSIGNED → Integer).
     * Maximum value: 16,777,215 bytes (~16 MB).
     * Nullable — may not be recorded for all documents.
     */
    private Integer fileSize;

    // =========================================================================
    // resident_documents verification fields
    // =========================================================================

    /**
     * Current verification status of the document.
     *
     * <p>Maps from {@code resident_documents.verification_status}
     * (ENUM('PENDING','VERIFIED','REJECTED') → VerificationStatus).
     * Defaults to {@code PENDING} on upload.
     */
    private VerificationStatus verificationStatus;

    /**
     * User ID of the person who uploaded this document.
     *
     * <p>Maps from {@code resident_documents.uploaded_by}
     * (INT UNSIGNED → Integer).
     * Can be the resident themselves or an admin.
     */
    private Integer uploadedBy;

    /**
     * User ID of the admin who verified or rejected this document.
     *
     * <p>Maps from {@code resident_documents.verified_by}
     * (INT UNSIGNED → Integer).
     * Null until an admin takes action on the document.
     */
    private Integer verifiedBy;

    /**
     * Timestamp when the document was verified or rejected.
     *
     * <p>Maps from {@code resident_documents.verified_at}
     * (TIMESTAMP NULL → LocalDateTime).
     * Null until admin takes action.
     * Always set together with {@link #verifiedBy}.
     */
    private LocalDateTime verifiedAt;

    /**
     * Timestamp when the document record was created (uploaded).
     *
     * <p>Maps from {@code resident_documents.created_at}
     * (TIMESTAMP DEFAULT CURRENT_TIMESTAMP → LocalDateTime).
     * Represents upload time — not verification time.
     */
    private LocalDateTime createdAt;
}
