package com.smartsocietyconnect.meeting.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartsocietyconnect.meeting.entity.Meeting;
import com.smartsocietyconnect.meeting.entity.MeetingMinutes;

/**
 * Repository for {@link MeetingMinutes} entity.
 *
 * <p>Provides database operations for Minutes of Meeting records.
 */
@Repository
public interface MeetingMinutesRepository
        extends JpaRepository<MeetingMinutes, Integer> {

    // ==========================================================================
    // 1. Meeting Minutes Queries
    // ==========================================================================

    /**
     * Finds minutes by meeting.
     *
     * @param meeting meeting entity
     * @return minutes record if found
     */
    Optional<MeetingMinutes> findByMeeting(Meeting meeting);

    /**
     * Checks whether minutes already exist for a meeting.
     *
     * @param meeting meeting entity
     * @return true if minutes already exist
     */
    boolean existsByMeeting(Meeting meeting);
}