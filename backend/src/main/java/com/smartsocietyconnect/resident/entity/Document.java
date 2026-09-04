package com.smartsocietyconnect.resident.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing an uploaded document in Smart Society Connect.
 *
 * <p>Maps to the {@code documents} table in the database.
 *
 * <p>Stores metadata about physical files uploaded by residents.
 * Actual file content is stored on disk or cloud storage — only
 * the path is persisted here.
 *
 * <p>This table is intentionally decoupled from residents — it is
 * linked via {@code resident_documents} join table, making it
 * reusable for vendor, security, and society documents in future.
 *
 * <p><b>Design notes:</b>
 * <ul>
 *     <li>{@code @AllArgsConstructor(access = AccessLevel.PRIVATE)} — required
 *         by Lombok {@code @Builder} internally; prevents direct all-args
 *         construction from outside the class</li>
 *     <li>{@code @Data} and {@code @EqualsAndHashCode} intentionally avoided
 *         to prevent JPA lazy-loading proxy issues</li>
 * </ul>
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see DocumentType
 * @see ResidentDocument
 */
@Entity
@Table(
    name = "documents",
    indexes = {
        @Index(name = "idx_document_type", columnList = "document_type")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Document {

    /**
     * Primary key for the document record.
     *
     * <p>Maps to {@code document_id} column (INT UNSIGNED, AUTO_INCREMENT).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Integer documentId;

    /**
     * Display name of the document.
     *
     * <p>Maps to {@code document_name} column (VARCHAR(100), NOT NULL).
     * Example: "Aadhaar Card", "PAN Card", "Rent Agreement 2024"
     */
    @Column(
        name     = "document_name",
        nullable = false,
        length   = 100
    )
    private String documentName;

    /**
     * Type/category of the document.
     *
     * <p>Maps to {@code document_type} column
     * (ENUM, NOT NULL).
     *
     * <p>{@code EnumType.STRING} stores the Java constant name directly —
     * all names match the MySQL ENUM values exactly, so no custom
     * {@code AttributeConverter} is needed.
     *
     * @see DocumentType
     */
    @Enumerated(EnumType.STRING)
    @Column(
        name             = "document_type",
        nullable         = false,
        columnDefinition = "ENUM('AADHAAR','PAN','PROOF_OF_ADDRESS'," +
                           "'OWNERSHIP','RENT_AGREEMENT','PROFILE_PHOTO','OTHER')"
    )
    private DocumentType documentType;

    /**
     * MIME type of the uploaded file.
     *
     * <p>Maps to {@code mime_type} column (VARCHAR(50), nullable).
     * Examples: "application/pdf", "image/jpeg", "image/png"
     */
    @Column(
        name   = "mime_type",
        length = 50
    )
    private String mimeType;

    /**
     * Relative or absolute path to the stored file.
     *
     * <p>Maps to {@code file_path} column (VARCHAR(255), NOT NULL).
     * Stores path on disk or object-storage key (e.g. S3 key).
     * Never stores binary content directly in the database.
     */
    @Column(
        name     = "file_path",
        nullable = false,
        length   = 255
    )
    private String filePath;

    /**
     * Size of the uploaded file in bytes.
     *
     * <p>Maps to {@code file_size} column (MEDIUMINT UNSIGNED, nullable).
     *
     * <p>MySQL MEDIUMINT UNSIGNED supports 0–16,777,215 bytes (up to ~16 MB),
     * which covers typical PDF and image uploads without the overhead of INT.
     *
     * <p>{@code columnDefinition} is required so Hibernate generates
     * {@code MEDIUMINT UNSIGNED} — without it, Hibernate defaults to
     * {@code INT} which wastes 1 byte per row and mismatches the DB schema.
     *
     * <p>Java {@code Integer} (32-bit signed, max ~2.1 billion) safely holds
     * any MEDIUMINT UNSIGNED value (max 16,777,215).
     */
    // ✅ FIXED: added columnDefinition to match DB MEDIUMINT UNSIGNED exactly
    @Column(
        name             = "file_size",
        columnDefinition = "MEDIUMINT UNSIGNED"
    )
    private Integer fileSize;

    /**
     * Timestamp when this document record was created.
     *
     * <p>Maps to {@code created_at} column (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP).
     * Set automatically by Hibernate on insert; never updated thereafter.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}