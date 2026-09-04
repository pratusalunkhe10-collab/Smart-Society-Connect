package com.smartsocietyconnect.resident.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartsocietyconnect.resident.entity.Document;
import com.smartsocietyconnect.resident.entity.DocumentType;

/**
 * Repository interface for {@link Document} entity.
 *
 * <p>Provides CRUD and query operations for the {@code documents} table.
 *
 * <p>Extends {@link JpaRepository} with generic types:
 * <ul>
 *     <li>{@link Document} — the entity type</li>
 *     <li>{@link Integer}  — the primary key type, matching {@code document_id} (INT UNSIGNED)</li>
 * </ul>
 *
 * <p>Spring Data JPA automatically provides standard operations including:
 * <ul>
 *     <li>{@code save(Document)}        — insert or update a document record</li>
 *     <li>{@code findById(Integer)}     — lookup by primary key</li>
 *     <li>{@code deleteById(Integer)}   — remove by primary key</li>
 *     <li>{@code count()}               — total document count</li>
 * </ul>
 *
 * <p><b>Design note:</b> The {@code documents} table is a standalone file
 * metadata registry — it stores document details (name, type, path, size)
 * independently of any resident. The association between documents and
 * residents is managed separately via the {@code resident_documents} join
 * table (see {@code ResidentDocumentRepository}). This design allows a
 * single document to be referenced by multiple residents if needed.
 *
 * <p>Used during:
 * <ul>
 *     <li>Document upload — store file metadata and prevent duplicate paths</li>
 *     <li>Document search — filter by type, MIME type, or file name</li>
 *     <li>Admin audit — list and review all documents of a specific category</li>
 * </ul>
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see Document
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Integer> {

    // ==========================================================================
    // Lookup Methods
    // ==========================================================================

    /**
     * Finds all documents of a specific category type.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM documents WHERE document_type = ?}</pre>
     *
     * <p>Backed by {@code idx_document_type} index on the {@code documents} table
     * for fast retrieval. Used in admin views to list all documents of a
     * given category (e.g. all {@link DocumentType#AADHAAR} records submitted
     * across the society).
     *
     * @param documentType the document category to filter by
     * @return a {@link List} of {@link Document}s of the specified type;
     *         never {@code null} — returns an empty list if none found
     */
    List<Document> findByDocumentType(DocumentType documentType);

    /**
     * Finds all documents with a specific MIME type.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM documents WHERE mime_type = ?}</pre>
     *
     * <p>Used to filter documents by file format, for example to retrieve
     * all PDFs ({@code "application/pdf"}) or all JPEG images
     * ({@code "image/jpeg"}) for format validation or file-serving logic.
     *
     * @param mimeType the MIME type to filter by (e.g. {@code "application/pdf"},
     *                 {@code "image/jpeg"})
     * @return a {@link List} of {@link Document}s with the specified MIME type;
     *         never {@code null} — returns an empty list if none found
     */
    List<Document> findByMimeType(String mimeType);

    /**
     * Finds all documents of a specific type and MIME type.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM documents WHERE document_type = ? AND mime_type = ?}</pre>
     *
     * <p>Used to validate that a document was uploaded in the expected format.
     * For example, verifying that all {@link DocumentType#AADHAAR} documents
     * are PDFs and not images — catching cases where a resident uploaded the
     * wrong file format for a given document category.
     *
     * @param documentType the document category to filter by
     * @param mimeType     the expected MIME type (e.g. {@code "application/pdf"})
     * @return a {@link List} of {@link Document}s matching both type and MIME type;
     *         never {@code null} — returns an empty list if none found
     */
    List<Document> findByDocumentTypeAndMimeType(
            DocumentType documentType,
            String mimeType
    );

    /**
     * Searches for documents whose name contains the given keyword,
     * case-insensitively.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT * FROM documents
     *WHERE LOWER(document_name) LIKE LOWER(CONCAT('%', ?, '%'))}</pre>
     *
     * <p>Used in admin search to locate documents by filename without knowing
     * the exact name. For example, searching {@code "aadhaar"} would match
     * {@code "Rahul_Aadhaar.pdf"}, {@code "AADHAAR_scan.jpg"} etc.
     *
     * <p><b>Performance note:</b> This query uses a {@code LIKE '%keyword%'}
     * pattern which cannot use a B-tree index. Acceptable for admin/low-volume
     * searches; consider full-text search for high-volume use cases.
     *
     * @param keyword the case-insensitive substring to search for in document names
     * @return a {@link List} of {@link Document}s whose name contains the keyword;
     *         never {@code null} — returns an empty list if none found
     */
    List<Document> findByDocumentNameContainingIgnoreCase(String keyword);

    // ==========================================================================
    // Existence Check Methods
    // ==========================================================================

    /**
     * Checks whether a document with the given file path already exists.
     *
     * <p>Spring Data JPA derives the query:
     * <pre>{@code SELECT COUNT(*) > 0 FROM documents WHERE file_path = ?}</pre>
     *
     * <p><b>Always call this before saving a new document</b> to prevent
     * duplicate file path entries — two records pointing to the same file
     * on disk would cause ambiguity during retrieval and deletion.
     *
     * @param filePath the file path or storage URL to check
     * @return {@code true} if a document with this path already exists,
     *         {@code false} otherwise
     */
    boolean existsByFilePath(String filePath);

}