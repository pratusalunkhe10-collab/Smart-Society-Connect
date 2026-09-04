package com.smartsocietyconnect.resident.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartsocietyconnect.resident.entity.ResidentDocument;
import com.smartsocietyconnect.resident.entity.VerificationStatus;

/**
 * Repository interface for {@link ResidentDocument} entity.
 *
 * <p>Provides CRUD and query operations for the {@code resident_documents} table.
 *
 * <p>Extends {@link JpaRepository} with generic types:
 * <ul>
 *     <li>{@link ResidentDocument} — the entity type</li>
 *     <li>{@link Integer}          — the primary key type,
 *         matching {@code resident_doc_id} (INT UNSIGNED)</li>
 * </ul>
 *
 * <p>Spring Data JPA automatically provides standard operations including:
 * <ul>
 *     <li>{@code save(ResidentDocument)}    — insert or update a mapping</li>
 *     <li>{@code findById(Integer)}         — lookup by primary key</li>
 *     <li>{@code deleteById(Integer)}       — remove a specific mapping</li>
 *     <li>{@code count()}                   — total mapping count</li>
 * </ul>
 *
 * <p><b>Design note:</b> {@code resident_documents} is an enriched join table —
 * it links {@link com.smartsocietyconnect.resident.entity.Resident} to
 * {@link com.smartsocietyconnect.resident.entity.Document} and adds extra
 * columns: {@code uploaded_by}, {@code verification_status}, {@code verified_by},
 * and {@code verified_at}. Because of these extra columns, it is modelled as a
 * full JPA {@code @Entity} rather than a simple {@code @ManyToMany} mapping.
 *
 * <p><b>Cascade deletion note:</b> The {@code resident_documents} table declares
 * {@code ON DELETE CASCADE} on the {@code resident_id} FK. Deleting a
 * {@link com.smartsocietyconnect.resident.entity.Resident} automatically removes
 * all associated {@link ResidentDocument} rows at the DB level. No
 * {@code deleteByResidentResidentId} method is needed here.
 *
 * <p>Used during:
 * <ul>
 *     <li>Document upload — link a document to a resident and track the uploader</li>
 *     <li>Verification workflow — find and update PENDING documents for admin review</li>
 *     <li>Admin dashboard — count documents by verification status for badge displays</li>
 *     <li>Audit trail — trace which admin verified/rejected a document and when</li>
 * </ul>
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see ResidentDocument
 * @see VerificationStatus
 */
@Repository
public interface ResidentDocumentRepository
        extends JpaRepository<ResidentDocument, Integer> {

    // ==========================================================================
    // Resident-based Lookup
    // ==========================================================================

    /**
     * Finds all document mappings for a specific resident.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM resident_documents WHERE resident_id = ?}</pre>
     *
     * <p>Used to display the full document history on a resident's profile —
     * including verified, pending, and rejected documents.
     *
     * @param residentId the {@code resident_id} of the target resident
     * @return a {@link List} of all {@link ResidentDocument} mappings for this
     *         resident; never {@code null} — returns an empty list if none found
     */
    List<ResidentDocument> findByResidentResidentId(Integer residentId);

    boolean existsByResidentDocIdAndResidentUserEmailIgnoreCase(
            Integer residentDocId,
            String email
    );

    /**
     * Finds all document mappings for a resident with a specific verification status.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM resident_documents
     *WHERE resident_id = ? AND verification_status = ?}</pre>
     *
     * <p>The primary use case is loading a resident's pending documents
     * to display what still requires admin review, or showing only
     * verified documents for a completed profile.
     *
     * @param residentId         the {@code resident_id} of the target resident
     * @param verificationStatus the status to filter by
     *                           ({@link VerificationStatus#PENDING},
     *                           {@link VerificationStatus#VERIFIED}, or
     *                           {@link VerificationStatus#REJECTED})
     * @return a {@link List} of matching {@link ResidentDocument}s;
     *         never {@code null} — returns an empty list if none found
     */
    List<ResidentDocument> findByResidentResidentIdAndVerificationStatus(
            Integer residentId,
            VerificationStatus verificationStatus
    );

    // ==========================================================================
    // Verification Status Queries
    // ==========================================================================

    /**
     * Finds all document mappings across the society with a given verification status.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM resident_documents WHERE verification_status = ?}</pre>
     *
     * <p>Used by the admin verification queue to load all
     * {@link VerificationStatus#PENDING} documents awaiting review.
     *
     * @param verificationStatus the status to filter by
     * @return a {@link List} of {@link ResidentDocument}s with the given status;
     *         never {@code null} — returns an empty list if none found
     */
    List<ResidentDocument> findByVerificationStatus(
            VerificationStatus verificationStatus
    );

    /**
     * Counts all document mappings with a given verification status.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT COUNT(*) FROM resident_documents
     *WHERE verification_status = ?}</pre>
     *
     * <p><b>Use this instead of</b>
     * {@code findByVerificationStatus(...).size()} for admin dashboard
     * badge counts (e.g. "12 documents pending") — this generates a
     * lightweight {@code COUNT} query and avoids loading all
     * {@link ResidentDocument} entities into memory just to count them.
     *
     * @param verificationStatus the status to count
     * @return the number of document mappings with the given status;
     *         {@code 0} if none found
     */
    long countByVerificationStatus(VerificationStatus verificationStatus);

    // ==========================================================================
    // Audit Trail Queries
    // ==========================================================================

    /**
     * Finds all document mappings uploaded by a specific user.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM resident_documents WHERE uploaded_by = ?}</pre>
     *
     * <p>Used for audit trail reporting to track which user uploaded
     * each document — useful when an admin or staff member uploads
     * documents on behalf of a resident.
     *
     * @param userId the {@code user_id} of the uploading
     *               {@link com.smartsocietyconnect.auth.entity.User}
     * @return a {@link List} of {@link ResidentDocument}s uploaded by this user;
     *         never {@code null} — returns an empty list if none found
     */
    List<ResidentDocument> findByUploadedByUserId(Integer userId);

    /**
     * Finds all document mappings verified or rejected by a specific admin user.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM resident_documents WHERE verified_by = ?}</pre>
     *
     * <p>Used for admin accountability reporting — tracks which administrator
     * actioned each document verification decision. Also useful for reviewing
     * an admin's verification history during audits.
     *
     * @param userId the {@code user_id} of the verifying admin
     *               {@link com.smartsocietyconnect.auth.entity.User}
     * @return a {@link List} of {@link ResidentDocument}s actioned by this admin;
     *         never {@code null} — returns an empty list if the admin has not
     *         verified or rejected any documents
     */
    List<ResidentDocument> findByVerifiedByUserId(Integer userId);

    // ==========================================================================
    // Existence Check Methods
    // ==========================================================================

    /**
     * Checks whether a specific document is already linked to a specific resident.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT COUNT(*) > 0 FROM resident_documents
     *WHERE resident_id = ? AND document_id = ?}</pre>
     *
     * <p><b>Always call this before saving</b> a new {@link ResidentDocument}
     * to prevent the same document from being linked to the same resident twice,
     * which would create a duplicate entry with no DB-level unique constraint
     * to catch it.
     *
     * @param residentId the {@code resident_id} to check
     * @param documentId the {@code document_id} to check
     * @return {@code true} if this document is already linked to this resident,
     *         {@code false} otherwise
     */
    boolean existsByResidentResidentIdAndDocumentDocumentId(
            Integer residentId,
            Integer documentId
    );

}
