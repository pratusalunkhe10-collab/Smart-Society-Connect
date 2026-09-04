package com.smartsocietyconnect.meeting.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.smartsocietyconnect.meeting.enums.AttendanceStatus;
import com.smartsocietyconnect.resident.entity.Resident;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a resident's RSVP response for a society meeting.
 *
 * <p>Maps to the {@code meeting_attendees} table in the database.
 *
 * <p>Each meeting can have many attendees, and each resident can attend many
 * meetings. This table resolves that many-to-many relationship while also
 * storing RSVP-specific data such as attendance status, remarks and response
 * time.
 *
 * <p><b>Business rule:</b> One resident can submit only one RSVP per meeting.
 * This is enforced by the database-level unique constraint on
 * {@code meeting_id} and {@code resident_id}.
 *
 * <p><b>Timestamp strategy:</b>
 * {@link CreationTimestamp} and {@link UpdateTimestamp} are used for audit
 * fields, while {@code respondedAt} is managed manually because it represents
 * a business event, not just a database update.
 */
@Entity
@Table(
        name = "meeting_attendees",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_meeting_resident",
                        columnNames = { "meeting_id", "resident_id" }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Access(AccessType.FIELD)
public class MeetingAttendance {

    // ==========================================================================
    // 1. Primary Key
    // ==========================================================================

    /**
     * Internal attendee record identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendee_id")
    private Integer attendeeId;

    // ==========================================================================
    // 2. Relationships
    // ==========================================================================

    /**
     * Meeting for which the resident is submitting an RSVP.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    /**
     * Resident who submitted the RSVP.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private Resident resident;

    // ==========================================================================
    // 3. RSVP Details
    // ==========================================================================

    /**
     * Current RSVP status of the resident.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false)
    private AttendanceStatus attendanceStatus = AttendanceStatus.PENDING;

    /**
     * Optional remarks added by the resident.
     */
    @Column(length = 255)
    private String remarks;

    /**
     * Time when the resident gave an actual RSVP response.
     *
     * <p>This remains null while the status is {@code PENDING}.
     */
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    // ==========================================================================
    // 4. Audit Fields
    // ==========================================================================

    /**
     * Timestamp when this attendee record was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this attendee record was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==========================================================================
    // 5. Entity Lifecycle Hooks
    // ==========================================================================

    /**
     * Ensures default RSVP state before insert.
     */
    @PrePersist
    public void prePersist() {

        if (attendanceStatus == null) {
            attendanceStatus = AttendanceStatus.PENDING;
        }

        updateRespondedAt();
    }

    /**
     * Updates response time when RSVP status changes.
     */
    @PreUpdate
    public void preUpdate() {
        updateRespondedAt();
    }

    /**
     * Sets respondedAt only when the resident has submitted a real response.
     */
    private void updateRespondedAt() {

        if (attendanceStatus != null
                && attendanceStatus != AttendanceStatus.PENDING
                && respondedAt == null) {

            respondedAt = LocalDateTime.now();
        }
    }
}