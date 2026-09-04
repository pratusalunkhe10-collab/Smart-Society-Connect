package com.smartsocietyconnect.resident.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import com.smartsocietyconnect.resident.dto.request.UploadDocumentRequest;
import com.smartsocietyconnect.resident.dto.response.DocumentResponse;
import com.smartsocietyconnect.resident.entity.VerificationStatus;

/**
 * Service interface for resident document management operations.
 *
 * <p>Defines business logic contracts for:
 * <ul>
 *     <li>Uploading documents and persisting file metadata</li>
 *     <li>Fetching documents by ID, resident, or verification status</li>
 *     <li>Verifying and rejecting documents (admin workflow)</li>
 *     <li>Deleting document records and physical files</li>
 * </ul>
 *
 * <p>Each upload creates two records atomically:
 * <ul>
 *     <li>{@code documents}          — file metadata (name, type, path, size, MIME)</li>
 *     <li>{@code resident_documents} — ownership + verification state</li>
 * </ul>
 *
 * <p>All methods return {@link DocumentResponse} DTOs — never raw entities —
 * to keep the API layer decoupled from the persistence layer.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see com.smartsocietyconnect.resident.entity.Document
 * @see com.smartsocietyconnect.resident.entity.ResidentDocument
 * @see VerificationStatus
 */
public interface DocumentService {

    // =========================================================================
    // UPLOAD
    // =========================================================================

    /**
     * Uploads a document for a resident and persists its metadata.
     *
     * <p>Flow:
     * <ol>
     *   <li>Validates resident exists</li>
     *   <li>Saves physical file to storage (disk or cloud)</li>
     *   <li>Persists file metadata to {@code documents} table</li>
     *   <li>Creates {@code resident_documents} record with
     *       {@link VerificationStatus#PENDING}</li>
     * </ol>
     *
     * <p>File path, MIME type, and size are derived from the
     * {@link MultipartFile} on the server — the client never
     * provides these values directly.
     *
     * @param request DTO containing residentId, documentType, documentName
     * @param file    the actual file binary received as multipart upload
     * @return {@link DocumentResponse} representing the uploaded document
     *         with initial status {@code PENDING}
     * @throws com.smartsocietyconnect.resident.exception.ResidentException
     *         if resident not found or file storage fails
     */
    DocumentResponse uploadDocument(
            UploadDocumentRequest request,
            MultipartFile file);

    // =========================================================================
    // READ
    // =========================================================================

    /**
     * Fetches a single document mapping record by its primary key.
     *
     * @param residentDocId the {@code resident_documents.resident_doc_id}
     * @return {@link DocumentResponse} for the matching record
     * @throws com.smartsocietyconnect.resident.exception.ResidentException
     *         if no record exists with the given ID
     */
    DocumentResponse getDocumentById(Integer residentDocId);

    /**
     * Fetches all documents uploaded by a specific resident.
     *
     * <p>Derived query on {@code resident_documents.resident_id}.
     * Returns documents of all verification statuses
     * ({@code PENDING}, {@code VERIFIED}, {@code REJECTED}).
     *
     * @param residentId the resident primary key
     * @return list of {@link DocumentResponse} for that resident;
     *         never {@code null} — returns empty list if none found
     * @throws com.smartsocietyconnect.resident.exception.ResidentException
     *         if resident not found
     */
    List<DocumentResponse> getDocumentsByResident(Integer residentId);

    /**
     * Fetches all document records filtered by verification status.
     *
     * <p>Used primarily by admin dashboard to list:
     * <ul>
     *     <li>{@link VerificationStatus#PENDING}  — awaiting admin review</li>
     *     <li>{@link VerificationStatus#VERIFIED} — approved documents</li>
     *     <li>{@link VerificationStatus#REJECTED} — rejected documents</li>
     * </ul>
     *
     * @param status the verification status to filter by
     * @return list of {@link DocumentResponse} with that status;
     *         never {@code null} — returns empty list if none found
     */
    List<DocumentResponse> getDocumentsByStatus(VerificationStatus status);

    /** Lists every resident document for the staff document register. */
    List<DocumentResponse> getAllDocuments();

    /** Loads the binary file associated with a resident document. */
    Resource loadDocumentFile(Integer residentDocId);

    // =========================================================================
    // VERIFICATION WORKFLOW
    // =========================================================================

    /**
     * Marks a document as {@link VerificationStatus#VERIFIED} by an admin.
     *
     * <p>Sets in {@code resident_documents}:
     * <ul>
     *     <li>{@code verification_status = 'VERIFIED'}</li>
     *     <li>{@code verified_by = verifierUserId}</li>
     *     <li>{@code verified_at = NOW()}</li>
     * </ul>
     *
     * <p>Only documents with status {@code PENDING} should be verified.
     * Attempting to verify an already-verified document may be rejected
     * by the implementation.
     *
     * @param residentDocId  the {@code resident_doc_id} to verify
     * @param verifierUserId the {@code user_id} of the admin performing verification
     * @return {@link DocumentResponse} with updated status {@code VERIFIED}
     * @throws com.smartsocietyconnect.resident.exception.ResidentException
     *         if document not found or verifier user not found
     */
    DocumentResponse verifyDocument(
            Integer residentDocId,
            Integer verifierUserId);

    /**
     * Marks a document as {@link VerificationStatus#REJECTED} by an admin.
     *
     * <p>Sets in {@code resident_documents}:
     * <ul>
     *     <li>{@code verification_status = 'REJECTED'}</li>
     *     <li>{@code verified_by = verifierUserId}</li>
     *     <li>{@code verified_at = NOW()}</li>
     * </ul>
     *
     * <p>After rejection, the resident should be notified to
     * re-upload a corrected document. The rejected record is
     * retained for audit purposes — not deleted.
     *
     * @param residentDocId  the {@code resident_doc_id} to reject
     * @param verifierUserId the {@code user_id} of the admin performing rejection
     * @return {@link DocumentResponse} with updated status {@code REJECTED}
     * @throws com.smartsocietyconnect.resident.exception.ResidentException
     *         if document not found or verifier user not found
     */
    DocumentResponse rejectDocument(
            Integer residentDocId,
            Integer verifierUserId);

    // =========================================================================
    // DELETE
    // =========================================================================

    /**
     * Deletes a document record and its associated physical file.
     *
     * <p>Steps performed:
     * <ol>
     *   <li>Deletes {@code resident_documents} record</li>
     *   <li>Deletes {@code documents} metadata record</li>
     *   <li>Deletes physical file from storage</li>
     * </ol>
     *
     * <p>All three steps should be performed atomically where possible.
     * If file deletion fails after DB deletion, the implementation
     * should log the orphaned file path for manual cleanup.
     *
     * @param residentDocId the {@code resident_doc_id} to delete
     * @throws com.smartsocietyconnect.resident.exception.ResidentException
     *         if no record exists with the given ID
     */
    void deleteDocument(Integer residentDocId);
}
