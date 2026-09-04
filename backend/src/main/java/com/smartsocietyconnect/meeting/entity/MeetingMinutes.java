package com.smartsocietyconnect.meeting.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.smartsocietyconnect.auth.entity.User;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing Minutes of Meeting (MOM).
 *
 * <p>Maps to the {@code meeting_minutes} table in the database.
 *
 * <p>Minutes are created after a meeting is completed or conducted. They store
 * the summary of discussion, decisions, and optionally a file path for an
 * uploaded MOM document.
 *
 * <p><b>Business rule:</b> Each meeting can have only one minutes record.
 * This is enforced by the unique constraint on {@code meeting_id}.
 *
 * <p><b>Relationship:</b>
 * One {@link Meeting} has one {@link MeetingMinutes}.
 *
 * <p><b>Timestamp strategy:</b>
 * {@link CreationTimestamp} and {@link UpdateTimestamp} are used so Hibernate
 * manages audit timestamps automatically.
 */
@Entity
@Table(name = "meeting_minutes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Access(AccessType.FIELD)
public class MeetingMinutes {

    // ==========================================================================
    // 1. Primary Key
    // ==========================================================================

    /**
     * Internal minutes identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "minutes_id")
    private Integer minutesId;

    // ==========================================================================
    // 2. Relationships
    // ==========================================================================

    /**
     * Meeting to which these minutes belong.
     *
     * <p>{@code unique = true} matches the database design where one meeting
     * can have only one MOM record.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false, unique = true)
    private Meeting meeting;

    /**
     * User who prepared the meeting minutes.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prepared_by", nullable = false)
    private User preparedBy;

    // ==========================================================================
    // 3. Minutes Details
    // ==========================================================================

    /**
     * Summary of meeting discussion, decisions and action points.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    /**
     * Optional uploaded MOM file path or stored file name.
     */
    @Column(name = "uploaded_file", length = 255)
    private String uploadedFile;

    // ==========================================================================
    // 4. Audit Fields
    // ==========================================================================

    /**
     * Timestamp when the minutes record was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the minutes record was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}