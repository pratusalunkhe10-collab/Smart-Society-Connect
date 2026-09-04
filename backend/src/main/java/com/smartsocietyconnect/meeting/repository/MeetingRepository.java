package com.smartsocietyconnect.meeting.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;

import com.smartsocietyconnect.meeting.entity.Meeting;
import com.smartsocietyconnect.meeting.enums.MeetingStatus;
import com.smartsocietyconnect.meeting.enums.MeetingAudience;

/**
 * Repository for {@link Meeting} entity.
 *
 * <p>Provides CRUD operations and query methods for meeting search,
 * filtering, status-based listing and dashboard-style lookups.
 */
@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Integer> {

    // ==========================================================================
    // 1. UUID Queries
    // ==========================================================================

    /**
     * Finds a meeting by its public UUID.
     *
     * @param meetingUuid public meeting UUID
     * @return meeting if found
     */
    Optional<Meeting> findByMeetingUuid(String meetingUuid);

    /**
     * Checks whether a meeting UUID already exists.
     *
     * @param meetingUuid public meeting UUID
     * @return true if UUID already exists
     */
    boolean existsByMeetingUuid(String meetingUuid);

    // ==========================================================================
    // 2. Status And Date Queries
    // ==========================================================================

    /**
     * Finds meetings by lifecycle status.
     *
     * @param status meeting status
     * @return matching meetings
     */
    List<Meeting> findByStatus(MeetingStatus status);

    /**
     * Finds meetings by lifecycle status ordered by latest meeting date first.
     *
     * @param status meeting status
     * @return matching meetings
     */
    List<Meeting> findByStatusOrderByMeetingDateDesc(MeetingStatus status);

    /**
     * Finds meetings scheduled on a specific date.
     *
     * @param meetingDate meeting date
     * @return meetings scheduled on the given date
     */
    List<Meeting> findByMeetingDate(LocalDate meetingDate);

    /**
     * Finds meetings from the given date onward.
     *
     * @param date starting date
     * @return upcoming meetings ordered by date
     */
    List<Meeting> findByMeetingDateGreaterThanEqualOrderByMeetingDateAsc(
            LocalDate date
    );

    /**
     * Finds meetings before the given date.
     *
     * @param date ending date
     * @return past meetings ordered by latest date first
     */
    List<Meeting> findByMeetingDateBeforeOrderByMeetingDateDesc(
            LocalDate date
    );

    // ==========================================================================
    // 3. Search And Listing Queries
    // ==========================================================================

    /**
     * Searches meetings by title.
     *
     * @param keyword search keyword
     * @return meetings whose title contains the keyword
     */
    List<Meeting> findByTitleContainingIgnoreCase(String keyword);

    /**
     * Returns all meetings ordered by latest created first.
     *
     * @return all meetings ordered by creation time descending
     */
    List<Meeting> findAllByOrderByCreatedAtDesc();

    List<Meeting> findByAudienceOrderByCreatedAtDesc(MeetingAudience audience);

    List<Meeting> findByAudienceAndMeetingDateGreaterThanEqualOrderByMeetingDateAsc(MeetingAudience audience, LocalDate date);

    List<Meeting> findByAudienceAndStatusOrderByMeetingDateDesc(MeetingAudience audience, MeetingStatus status);

    List<Meeting> findByAudienceAndTitleContainingIgnoreCase(MeetingAudience audience, String keyword);
    
    // ==========================================================================
    // 4. Duplicate Validation Queries
    // ==========================================================================

    boolean existsByTitleIgnoreCaseAndMeetingDateAndStartTime(
            String title,
            LocalDate meetingDate,
            LocalTime startTime
    );
}
