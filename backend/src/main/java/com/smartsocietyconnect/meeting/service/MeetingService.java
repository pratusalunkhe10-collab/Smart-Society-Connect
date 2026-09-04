package com.smartsocietyconnect.meeting.service;

import java.util.List;

import com.smartsocietyconnect.meeting.dto.request.CreateMeetingRequest;
import com.smartsocietyconnect.meeting.dto.request.MeetingMinutesRequest;
import com.smartsocietyconnect.meeting.dto.request.UpdateMeetingRequest;
import com.smartsocietyconnect.meeting.dto.response.MeetingDetailsResponse;
import com.smartsocietyconnect.meeting.dto.response.MeetingMinutesResponse;
import com.smartsocietyconnect.meeting.dto.response.MeetingResponse;
import com.smartsocietyconnect.meeting.enums.MeetingStatus;

/**
 * Service interface for Meeting Module.
 *
 * <p>Defines business operations for creating meetings, updating schedules,
 * changing meeting status, managing Minutes of Meeting and reading meeting
 * records.
 */
public interface MeetingService {

    // ==========================================================================
    // 1. Meeting Write Operations
    // ==========================================================================

    /**
     * Creates a new society meeting.
     *
     * @param request meeting creation request
     * @return created meeting response
     */
    MeetingResponse createMeeting(CreateMeetingRequest request);

    /**
     * Updates an existing meeting.
     *
     * @param meetingUuid public meeting UUID
     * @param request meeting update request
     * @return updated meeting response
     */
    MeetingResponse updateMeeting(
            String meetingUuid,
            UpdateMeetingRequest request
    );

    /**
     * Updates meeting lifecycle status.
     *
     * @param meetingUuid public meeting UUID
     * @param status requested meeting status
     * @return updated meeting response
     */
    MeetingResponse updateMeetingStatus(
            String meetingUuid,
            MeetingStatus status
    );

    /**
     * Cancels a meeting.
     *
     * @param meetingUuid public meeting UUID
     * @return cancelled meeting response
     */
    MeetingResponse cancelMeeting(String meetingUuid);

    /**
     * Deletes a meeting.
     *
     * @param meetingUuid public meeting UUID
     */
    void deleteMeeting(String meetingUuid);

    // ==========================================================================
    // 2. Meeting Read Operations
    // ==========================================================================

    /**
     * Gets detailed meeting information.
     *
     * @param meetingUuid public meeting UUID
     * @return meeting details response
     */
    MeetingDetailsResponse getMeetingByUuid(String meetingUuid);

    /**
     * Gets all meetings.
     *
     * @return all meeting responses
     */
    List<MeetingResponse> getAllMeetings();

    /**
     * Gets upcoming meetings from today onward.
     *
     * @return upcoming meeting responses
     */
    List<MeetingResponse> getUpcomingMeetings();

    /**
     * Gets completed meetings.
     *
     * @return completed meeting responses
     */
    List<MeetingResponse> getCompletedMeetings();

    /**
     * Gets meetings by status.
     *
     * @param status meeting status
     * @return matching meeting responses
     */
    List<MeetingResponse> getMeetingsByStatus(MeetingStatus status);

    /**
     * Searches meetings by title keyword.
     *
     * @param keyword title search keyword
     * @return matching meeting responses
     */
    List<MeetingResponse> searchMeetings(String keyword);

    // ==========================================================================
    // 3. Meeting Minutes Operations
    // ==========================================================================

    /**
     * Creates or updates Minutes of Meeting.
     *
     * @param meetingUuid public meeting UUID
     * @param request meeting minutes request
     * @return meeting minutes response
     */
    MeetingMinutesResponse uploadMeetingMinutes(
            String meetingUuid,
            MeetingMinutesRequest request
    );

    /**
     * Gets Minutes of Meeting.
     *
     * @param meetingUuid public meeting UUID
     * @return meeting minutes response
     */
    MeetingMinutesResponse getMeetingMinutes(String meetingUuid);
}