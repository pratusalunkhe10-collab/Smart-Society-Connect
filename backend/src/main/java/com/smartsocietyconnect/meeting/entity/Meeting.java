package com.smartsocietyconnect.meeting.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.meeting.enums.MeetingStatus;
import com.smartsocietyconnect.meeting.enums.MeetingAudience;
import com.smartsocietyconnect.meeting.enums.MeetingType;
import com.smartsocietyconnect.meeting.enums.MeetingMode;

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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a society meeting.
 *
 * <p>Maps to the {@code meetings} table in the database.
 *
 * <p>A meeting is created by an authorized user such as ADMIN or SECRETARY.
 * Residents can respond to the meeting through the {@code meeting_attendees}
 * table, and the meeting minutes are stored separately in the
 * {@code meeting_minutes} table.
 *
 * <p><b>Why meetingUuid exists:</b>
 * {@code meeting_id} is the internal database primary key. {@code meeting_uuid}
 * is safer for public APIs because it prevents predictable ID enumeration.
 *
 * <p><b>Timestamp strategy:</b>
 * {@link CreationTimestamp} and {@link UpdateTimestamp} are used so Hibernate
 * manages audit timestamps automatically.
 *
 * <p><b>Lombok strategy:</b>
 * {@code @Data} is intentionally avoided because JPA entities should not
 * generate equals/hashCode/toString automatically when lazy relationships exist.
 */
@Entity
@Table(name = "meetings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Meeting {

    // ==========================================================================
    // 1. Primary Key
    // ==========================================================================

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meeting_id")
    private Integer meetingId;

    /**
     * Public unique identifier used in APIs.
     */
    @Column(
            name = "meeting_uuid",
            nullable = false,
            unique = true,
            columnDefinition = "CHAR(36)",
            updatable = false
    )
    private String meetingUuid;

    // ==========================================================================
    // 2. Meeting Details
    // ==========================================================================

    /**
     * Short meeting title.
     */
    @Column(nullable = false, length = 100)
    private String title;

    /**
     * Optional detailed meeting description.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Optional meeting agenda.
     */
    @Column(columnDefinition = "TEXT")
    private String agenda;

    /** Purpose classification used for filtering and notices. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_type", nullable = false, length = 30)
    private MeetingType meetingType = MeetingType.GENERAL_BODY;

    /**
     * Date on which the meeting is scheduled.
     */
    @Column(name = "meeting_date", nullable = false)
    private LocalDate meetingDate;

    /**
     * Meeting start time.
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * Meeting end time.
     */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Physical or virtual meeting venue.
     */
    @Column(nullable = false, length = 150)
    private String venue;

    /** Whether residents attend at the venue, online, or both. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_mode", nullable = false, length = 20)
    private MeetingMode meetingMode = MeetingMode.IN_PERSON;

    /** Optional HTTPS link for online or hybrid meetings. */
    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    // ==========================================================================
    // 3. Meeting Status
    // ==========================================================================

    /**
     * Current meeting lifecycle status.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingStatus status = MeetingStatus.SCHEDULED;

    /** Audience controls both notification delivery and resident visibility. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MeetingAudience audience = MeetingAudience.ALL_RESIDENTS;

    // ==========================================================================
    // 4. Relationships
    // ==========================================================================

    /**
     * User who created the meeting.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // ==========================================================================
    // 5. Audit Fields
    // ==========================================================================

    /**
     * Timestamp when the meeting was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the meeting was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==========================================================================
    // 6. Entity Lifecycle Hooks
    // ==========================================================================

    /**
     * Generates a UUID before inserting a new meeting.
     */
    @PrePersist
    public void prePersist() {

        if (meetingUuid == null || meetingUuid.isBlank()) {
            meetingUuid = UUID.randomUUID().toString();
        }

        if (status == null) {
            status = MeetingStatus.SCHEDULED;
        }
        if (audience == null) {
            audience = MeetingAudience.ALL_RESIDENTS;
        }
        if (meetingType == null) {
            meetingType = MeetingType.GENERAL_BODY;
        }
        if (meetingMode == null) {
            meetingMode = MeetingMode.IN_PERSON;
        }
    }
}
