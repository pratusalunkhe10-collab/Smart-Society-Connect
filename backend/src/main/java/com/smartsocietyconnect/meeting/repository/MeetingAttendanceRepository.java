package com.smartsocietyconnect.meeting.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartsocietyconnect.meeting.entity.Meeting;
import com.smartsocietyconnect.meeting.entity.MeetingAttendance;
import com.smartsocietyconnect.meeting.enums.AttendanceStatus;
import com.smartsocietyconnect.resident.entity.Resident;

/**
 * Repository for {@link MeetingAttendance} entity.
 *
 * <p>Handles RSVP records for residents attending meetings.
 */
@Repository
public interface MeetingAttendanceRepository
        extends JpaRepository<MeetingAttendance, Integer> {

    // ==========================================================================
    // 1. RSVP Lookup Queries
    // ==========================================================================

    /**
     * Finds a resident's RSVP for a meeting.
     *
     * @param meeting meeting entity
     * @param resident resident entity
     * @return RSVP record if found
     */
    Optional<MeetingAttendance> findByMeetingAndResident(
            Meeting meeting,
            Resident resident
    );

    /**
     * Checks whether a resident already has an RSVP record for a meeting.
     *
     * @param meeting meeting entity
     * @param resident resident entity
     * @return true if RSVP already exists
     */
    boolean existsByMeetingAndResident(
            Meeting meeting,
            Resident resident
    );

    // ==========================================================================
    // 2. Meeting Attendee Queries
    // ==========================================================================

    /**
     * Gets all attendees for a meeting.
     *
     * @param meeting meeting entity
     * @return attendee records
     */
    List<MeetingAttendance> findByMeeting(Meeting meeting);

    /**
     * Gets all attendees for a meeting filtered by RSVP status.
     *
     * @param meeting meeting entity
     * @param attendanceStatus RSVP status
     * @return matching attendee records
     */
    List<MeetingAttendance> findByMeetingAndAttendanceStatus(
            Meeting meeting,
            AttendanceStatus attendanceStatus
    );

    /**
     * Counts attendees for a meeting filtered by RSVP status.
     *
     * @param meeting meeting entity
     * @param attendanceStatus RSVP status
     * @return count of matching attendees
     */
    long countByMeetingAndAttendanceStatus(
            Meeting meeting,
            AttendanceStatus attendanceStatus
    );

    /**
     * Counts all attendees linked to a meeting.
     *
     * @param meeting meeting entity
     * @return total attendee count
     */
    long countByMeeting(Meeting meeting);

    // ==========================================================================
    // 3. Resident RSVP Queries
    // ==========================================================================

    /**
     * Finds all RSVP records submitted by a resident.
     *
     * @param resident resident entity
     * @return RSVP records
     */
    List<MeetingAttendance> findByResident(Resident resident);
}