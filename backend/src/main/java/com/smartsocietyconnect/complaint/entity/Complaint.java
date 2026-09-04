package com.smartsocietyconnect.complaint.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.smartsocietyconnect.complaint.enums.ComplaintCategory;
import com.smartsocietyconnect.complaint.enums.ComplaintPriority;
import com.smartsocietyconnect.complaint.enums.ComplaintStatus;
import com.smartsocietyconnect.resident.entity.Resident;

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
 * Entity representing a resident complaint in Smart Society Connect.
 *
 * <p>Maps to the {@code complaints} table in the database.
 *
 * <p>A complaint is created by a resident and can move through a workflow such as:
 * <pre>
 * OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED
 * OPEN -> REJECTED
 * </pre>
 *
 * <p><b>Database design notes:</b>
 * <ul>
 *     <li>{@code complaint_id} is the primary key.</li>
 *     <li>{@code complaint_number} is a public unique tracking number.</li>
 *     <li>{@code resident_id} links the complaint to the resident who raised it.</li>
 *     <li>{@code category}, {@code priority}, and {@code status} are MySQL ENUM columns.</li>
 *     <li>{@code created_at} and {@code updated_at} are audit columns.</li>
 * </ul>
 *
 * <p><b>Lombok constructor strategy:</b>
 * <ul>
 *     <li>{@code @NoArgsConstructor} is required by JPA.</li>
 *     <li>{@code @AllArgsConstructor(access = AccessLevel.PRIVATE)} supports
 *         Lombok builder while preventing direct all-args construction.</li>
 *     <li>{@code @Builder} provides clean object creation.</li>
 * </ul>
 */
@Entity
@Table(
        name = "complaints",
        indexes = {
                @Index(name = "idx_complaint_resident", columnList = "resident_id"),
                @Index(name = "idx_complaint_status", columnList = "status"),
                @Index(name = "idx_complaint_priority", columnList = "priority"),
                @Index(name = "idx_complaint_category", columnList = "category"),
                @Index(name = "idx_complaint_created", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Complaint {

    // ==========================================================================
    // Primary Key
    // ==========================================================================

    /**
     * Primary key for the complaint.
     *
     * <p>Maps to {@code complaints.complaint_id}.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "complaint_id", columnDefinition = "INT UNSIGNED")
    private Integer complaintId;

    // ==========================================================================
    // Complaint Identifier
    // ==========================================================================

    /**
     * Public unique complaint number used for tracking.
     *
     * <p>Example: {@code CMP-20260701-0001}.
     *
     * <p>This should be generated in the service layer before saving.
     */
    @Column(name = "complaint_number", nullable = false, unique = true, length = 20)
    private String complaintNumber;

    // ==========================================================================
    // Resident Relationship
    // ==========================================================================

    /**
     * Resident who raised the complaint.
     *
     * <p>Maps to {@code complaints.resident_id}. The database uses
     * {@code ON DELETE RESTRICT}, so residents with complaint history
     * cannot be deleted accidentally.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resident_id", nullable = false)
    private Resident resident;

    // ==========================================================================
    // Complaint Details
    // ==========================================================================

    /**
     * Short title of the complaint.
     */
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /**
     * Detailed complaint description.
     *
     * <p>Maps to a MySQL {@code TEXT} column.
     */
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Complaint category.
     *
     * <p>Maps to MySQL ENUM:
     * PLUMBING, ELECTRICAL, LIFT, PARKING, SECURITY, HOUSEKEEPING,
     * WATER, GARDEN, OTHER.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "category",
            nullable = false,
            columnDefinition = "ENUM('PLUMBING','ELECTRICAL','LIFT','PARKING','SECURITY','HOUSEKEEPING','WATER','GARDEN','OTHER')"
    )
    private ComplaintCategory category;

    /**
     * Complaint priority.
     *
     * <p>Defaults to {@link ComplaintPriority#MEDIUM}.
     *
     * <p>{@code @Builder.Default} is required because Lombok builder otherwise
     * ignores field initializer defaults.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "priority",
            nullable = false,
            columnDefinition = "ENUM('LOW','MEDIUM','HIGH','URGENT') DEFAULT 'MEDIUM'"
    )
    private ComplaintPriority priority = ComplaintPriority.MEDIUM;

    /**
     * Current complaint status.
     *
     * <p>Defaults to {@link ComplaintStatus#OPEN}.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            columnDefinition = "ENUM('OPEN','IN_PROGRESS','RESOLVED','CLOSED','REJECTED') DEFAULT 'OPEN'"
    )
    private ComplaintStatus status = ComplaintStatus.OPEN;

    /**
     * Optional attachment path or URL.
     *
     * <p>If multiple attachments are needed later, create a separate
     * complaint_attachments table instead of adding more columns.
     */
    @Column(name = "attachment", length = 255)
    private String attachment;

    /**
     * Staff/team/person assigned to handle the complaint.
     *
     * <p>Currently stored as text. If your project later has staff users,
     * this can be replaced with a foreign key to {@code users.user_id}.
     */
    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    // ==========================================================================
    // Resolution Details
    // ==========================================================================

    /**
     * Remarks added when resolving, closing, or rejecting a complaint.
     */
    @Column(name = "resolution_remarks", columnDefinition = "TEXT")
    private String resolutionRemarks;

    /**
     * Timestamp when complaint was resolved.
     *
     * <p>Should be set by the service layer when status becomes RESOLVED.
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // ==========================================================================
    // Audit Fields
    // ==========================================================================

    /**
     * Timestamp when complaint was created.
     *
     * <p>Automatically populated by Hibernate on insert.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when complaint was last updated.
     *
     * <p>Automatically updated by Hibernate on update.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}