package com.smartsocietyconnect.meeting.service;

import java.util.List;

import com.smartsocietyconnect.meeting.dto.request.MeetingAttendanceRequest;
import com.smartsocietyconnect.meeting.dto.response.MeetingAttendanceResponse;

/**
 * Service interface for Meeting Attendance operations.
 *
 * <p>Handles RSVP submission, RSVP updates, attendee listing and RSVP summary
 * counts.
 */
public interface MeetingAttendanceService {

    // ==========================================================================
    // 1. RSVP Write Operations
    // ==========================================================================

    /**
     * Submits a resident RSVP for a meeting.
     *
     * @param meetingUuid public meeting UUID
     * @param residentId resident ID
     * @param request attendance request
     * @return submitted RSVP response
     */
    MeetingAttendanceResponse submitAttendance(
            String meetingUuid,
            Integer residentId,
            MeetingAttendanceRequest request
    );

    /**
     * Updates an existing resident RSVP for a meeting.
     *
     * @param meetingUuid public meeting UUID
     * @param residentId resident ID
     * @param request attendance request
     * @return updated RSVP response
     */
    MeetingAttendanceResponse updateAttendance(
            String meetingUuid,
            Integer residentId,
            MeetingAttendanceRequest request
    );

    /**
     * Deletes a resident RSVP for a meeting.
     *
     * @param meetingUuid public meeting UUID
     * @param residentId resident ID
     */
    void deleteAttendance(
            String meetingUuid,
            Integer residentId
    );

    // ==========================================================================
    // 2. RSVP Read Operations
    // ==========================================================================

    /**
     * Gets all attendees of a meeting.
     *
     * @param meetingUuid public meeting UUID
     * @return attendee responses
     */
    List<MeetingAttendanceResponse> getMeetingAttendees(
            String meetingUuid
    );

    /** Gets one resident's own RSVP without exposing other residents' responses. */
    MeetingAttendanceResponse getResidentAttendance(String meetingUuid, Integer residentId);

    /**
     * Gets RSVP history of a resident.
     *
     * @param residentId resident ID
     * @return resident RSVP responses
     */
    List<MeetingAttendanceResponse> getResidentAttendances(
            Integer residentId
    );

    // ==========================================================================
    // 3. RSVP Count Operations
    // ==========================================================================

    /**
     * Counts residents who are going.
     *
     * @param meetingUuid public meeting UUID
     * @return GOING count
     */
    long countGoing(String meetingUuid);

    /**
     * Counts residents who are not going.
     *
     * @param meetingUuid public meeting UUID
     * @return NOT_GOING count
     */
    long countNotGoing(String meetingUuid);

    /**
     * Counts residents who may attend.
     *
     * @param meetingUuid public meeting UUID
     * @return MAYBE count
     */
    long countMaybe(String meetingUuid);

    /**
     * Counts residents who have not responded yet.
     *
     * @param meetingUuid public meeting UUID
     * @return PENDING count
     */
    long countPending(String meetingUuid);
}
