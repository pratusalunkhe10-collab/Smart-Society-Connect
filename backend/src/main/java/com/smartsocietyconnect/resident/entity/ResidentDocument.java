package com.smartsocietyconnect.resident.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.smartsocietyconnect.auth.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing the mapping between a resident and their uploaded documents.
 *
 * <p>Maps to the {@code resident_documents} table in the database.
 *
 * <p>This is a join table between {@link Resident} and {@link Document},
 * extended with verification workflow fields:
 * <ul>
 *     <li>Who uploaded the document ({@code uploaded_by})</li>
 *     <li>Current verification status ({@code verification_status})</li>
 *     <li>Who verified it and when ({@code verified_by}, {@code verified_at})</li>
 * </ul>
 *
 * <p><b>Why separate {@code documents} and {@code resident_documents}?</b>
 * The {@code documents} table stores only file metadata (path, type, size).
 * This table links documents to residents and tracks the verification lifecycle.
 * This design makes the {@code documents} table reusable for vendors,
 * security staff, and society-level documents in future modules.
 *
 * <p><b>Indexes:</b>
 * <ul>
 *     <li>{@code idx_resident_doc_resident} — fast lookup of all docs for a resident</li>
 *     <li>{@code idx_resident_doc_document} — fast reverse lookup by document</li>
 *     <li>{@code idx_resident_doc_status}   — fast filtering by verification status (admin dashboard)</li>
 * </ul>
 *
 * <p><b>Design notes:</b>
 * <ul>
 *     <li>{@code @AllArgsConstructor(access = AccessLevel.PRIVATE)} — required
 *         internally by Lombok {@code @Builder}; blocked from external use</li>
 *     <li>{@code @Data} and {@code @EqualsAndHashCode} intentionally avoided
 *         to prevent issues with JPA lazy-loading proxies</li>
 *     <li>All {@code @ManyToOne} use {@code FetchType.LAZY} to avoid loading
 *         the full object graph on every query</li>
 * </ul>
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see Resident
 * @see Document
 * @see VerificationStatus
 */
@Entity
@Table(
    name = "resident_documents",
    indexes = {
        @Index(name = "idx_resident_doc_resident", columnList = "resident_id"),
        @Index(name = "idx_resident_doc_document", columnList = "document_id"),
        @Index(name = "idx_resident_doc_status",   columnList = "verification_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ResidentDocument {

    /**
     * Primary key for the resident-document mapping record.
     *
     * <p>Maps to {@code resident_doc_id} column (INT UNSIGNED, AUTO_INCREMENT).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resident_doc_id")
    private Integer residentDocId;

    /**
     * The resident who owns this document.
     *
     * <p>Maps to {@code resident_id} foreign key column (INT UNSIGNED, NOT NULL).
     * References {@code residents(resident_id)} with {@code ON DELETE CASCADE} —
     * if the resident is deleted, all their document mappings are removed automatically.
     *
     * <p>{@code optional = false} aligns the ORM constraint with the DB {@code NOT NULL}.
     * {@code FetchType.LAZY} avoids loading the full {@link Resident} graph on
     * every document lookup.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resident_id", nullable = false)
    private Resident resident;

    /**
     * The document record linked to this resident.
     *
     * <p>Maps to {@code document_id} foreign key column (INT UNSIGNED, NOT NULL).
     * References {@code documents(document_id)}.
     *
     * <p>No cascade — the {@link Document} record exists independently
     * and may be referenced by other entities in future modules.
     *
     * <p>{@code optional = false} aligns the ORM constraint with the DB {@code NOT NULL}.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    /**
     * The user who uploaded this document.
     *
     * <p>Maps to {@code uploaded_by} foreign key column (INT UNSIGNED, NOT NULL).
     * References {@code users(user_id)}.
     *
     * <p>Can be the resident themselves or an admin uploading on their behalf.
     * {@code optional = false} aligns the ORM constraint with the DB {@code NOT NULL}.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    /**
     * Current verification status of the document.
     *
     * <p>Maps to {@code verification_status} column
     * (ENUM('PENDING','VERIFIED','REJECTED'), DEFAULT 'PENDING', NOT NULL).
     *
     * <p>Lifecycle:
     * <pre>
     * PENDING → VERIFIED  (admin approves)
     * PENDING → REJECTED  (admin rejects, resident must re-upload)
     * </pre>
     *
     * <p>{@code @Builder.Default} ensures {@code PENDING} is set automatically
     * when using the Lombok builder — without it, {@code @Builder} silently
     * sets the field to {@code null}, violating the {@code NOT NULL} DB constraint.
     *
     * <p>{@code EnumType.STRING} stores the constant name directly.
     * All Java constant names match the MySQL ENUM values exactly, so
     * no custom {@code AttributeConverter} is needed.
     *
     * @see VerificationStatus
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
        name             = "verification_status",
        nullable         = false,
        columnDefinition = "ENUM('PENDING','VERIFIED','REJECTED') DEFAULT 'PENDING'"
    )
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    /**
     * The admin user who verified or rejected this document.
     *
     * <p>Maps to {@code verified_by} foreign key column (INT UNSIGNED, nullable).
     * References {@code users(user_id)}.
     *
     * <p>Remains {@code null} until an admin takes action on the document.
     * {@code optional} defaults to {@code true} — correct since the column is nullable.
     *
     * <p>Should always be set together with {@link #verifiedAt} and
     * {@link #verificationStatus} during the verification workflow.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    /**
     * Timestamp when the document was verified or rejected.
     *
     * <p>Maps to {@code verified_at} column (TIMESTAMP NULL).
     *
     * <p>Remains {@code null} until admin takes action.
     * Set programmatically when {@link #verificationStatus} changes
     * from {@code PENDING} to {@code VERIFIED} or {@code REJECTED}.
     *
     * <p>Should always be set together with {@link #verifiedBy}.
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /**
     * Timestamp when this resident-document mapping was created.
     *
     * <p>Maps to {@code created_at} column (TIMESTAMP DEFAULT CURRENT_TIMESTAMP).
     * Set automatically by Hibernate on insert; never updated thereafter.
     *
     * <p>Represents when the document was first uploaded and linked
     * to the resident — not when it was verified.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}