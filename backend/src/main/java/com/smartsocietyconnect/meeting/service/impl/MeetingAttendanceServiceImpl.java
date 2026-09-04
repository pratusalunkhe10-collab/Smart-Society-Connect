package com.smartsocietyconnect.meeting.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.smartsocietyconnect.meeting.dto.request.MeetingAttendanceRequest;
import com.smartsocietyconnect.meeting.dto.response.MeetingAttendanceResponse;
import com.smartsocietyconnect.meeting.entity.Meeting;
import com.smartsocietyconnect.meeting.entity.MeetingAttendance;
import com.smartsocietyconnect.meeting.enums.AttendanceStatus;
import com.smartsocietyconnect.meeting.enums.MeetingStatus;
import com.smartsocietyconnect.meeting.exception.AttendanceAlreadySubmittedException;
import com.smartsocietyconnect.meeting.exception.MeetingException;
import com.smartsocietyconnect.meeting.exception.MeetingNotFoundException;
import com.smartsocietyconnect.meeting.mapper.MeetingMapper;
import com.smartsocietyconnect.meeting.repository.MeetingAttendanceRepository;
import com.smartsocietyconnect.meeting.repository.MeetingRepository;
import com.smartsocietyconnect.meeting.service.MeetingAttendanceService;
import com.smartsocietyconnect.resident.entity.Resident;
import com.smartsocietyconnect.resident.exception.ResidentException;
import com.smartsocietyconnect.resident.repository.ResidentRepository;
import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for Meeting Attendance operations.
 *
 * <p>Handles RSVP submission, RSVP updates, attendee listing, resident RSVP
 * history and RSVP count operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MeetingAttendanceServiceImpl implements MeetingAttendanceService {

    private final MeetingRepository meetingRepository;
    private final MeetingAttendanceRepository meetingAttendanceRepository;
    private final ResidentRepository residentRepository;
    private final UserRepository userRepository;
    private final MeetingMapper meetingMapper;

    // ==========================================================================
    // 1. RSVP Write Operations
    // ==========================================================================

    /**
     * Submits a resident RSVP for a meeting.
     */
    @Override
    public MeetingAttendanceResponse submitAttendance(
            String meetingUuid,
            Integer residentId,
            MeetingAttendanceRequest request
    ) {

        validateAttendanceRequest(request);

        Meeting meeting = findMeetingByUuid(meetingUuid);
        Resident resident = findResidentById(residentId);
        requireResidentOwnership(resident);

        validateMeetingAllowsAttendance(
                meeting,
                "Cannot submit RSVP for this meeting."
        );

        if (meetingAttendanceRepository.existsByMeetingAndResident(
                meeting,
                resident
        )) {

            throw new AttendanceAlreadySubmittedException(
                    residentId,
                    meetingUuid
            );
        }

        MeetingAttendance attendance = MeetingAttendance.builder()
                .meeting(meeting)
                .resident(resident)
                .attendanceStatus(request.getAttendanceStatus())
                .remarks(request.getRemarks())
                .build();

        MeetingAttendance savedAttendance =
                meetingAttendanceRepository.save(attendance);

        log.info(
                "RSVP submitted successfully. meetingUuid={}, residentId={}, status={}",
                meetingUuid,
                residentId,
                request.getAttendanceStatus()
        );

        return meetingMapper.toMeetingAttendanceResponse(savedAttendance);
    }

    /**
     * Updates an existing RSVP for a resident.
     */
    @Override
    public MeetingAttendanceResponse updateAttendance(
            String meetingUuid,
            Integer residentId,
            MeetingAttendanceRequest request
    ) {

        validateAttendanceRequest(request);

        Meeting meeting = findMeetingByUuid(meetingUuid);
        Resident resident = findResidentById(residentId);
        requireResidentOwnership(resident);

        validateMeetingAllowsAttendance(
                meeting,
                "Cannot update RSVP for this meeting."
        );

        MeetingAttendance attendance = findAttendance(
                meeting,
                resident
        );

        attendance.setAttendanceStatus(request.getAttendanceStatus());
        attendance.setRemarks(request.getRemarks());

        MeetingAttendance updatedAttendance =
                meetingAttendanceRepository.save(attendance);

        log.info(
                "RSVP updated successfully. meetingUuid={}, residentId={}, status={}",
                meetingUuid,
                residentId,
                request.getAttendanceStatus()
        );

        return meetingMapper.toMeetingAttendanceResponse(updatedAttendance);
    }

    /**
     * Deletes an existing RSVP.
     *
     * <p>Deletion is blocked after meeting completion because completed meeting
     * attendance records are historical data.
     */
    @Override
    public void deleteAttendance(
            String meetingUuid,
            Integer residentId
    ) {

        Meeting meeting = findMeetingByUuid(meetingUuid);
        Resident resident = findResidentById(residentId);
        requireResidentOwnership(resident);

        if (meeting.getStatus() == MeetingStatus.COMPLETED) {
            throw new MeetingException(
                    "Cannot delete RSVP for a completed meeting."
            );
        }

        MeetingAttendance attendance = findAttendance(
                meeting,
                resident
        );

        meetingAttendanceRepository.delete(attendance);

        log.info(
                "RSVP deleted successfully. meetingUuid={}, residentId={}",
                meetingUuid,
                residentId
        );
    }

    // ==========================================================================
    // 2. RSVP Read Operations
    // ==========================================================================

    /**
     * Gets all attendees of a meeting.
     */
    @Override
    @Transactional(readOnly = true)
    public List<MeetingAttendanceResponse> getMeetingAttendees(
            String meetingUuid
    ) {

        Meeting meeting = findMeetingByUuid(meetingUuid);

        return meetingAttendanceRepository.findByMeeting(meeting)
                .stream()
                .map(meetingMapper::toMeetingAttendanceResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingAttendanceResponse getResidentAttendance(String meetingUuid, Integer residentId) {
        Meeting meeting = findMeetingByUuid(meetingUuid);
        Resident resident = findResidentById(residentId);
        requireResidentOwnership(resident);
        return meetingAttendanceRepository.findByMeetingAndResident(meeting, resident)
                .map(meetingMapper::toMeetingAttendanceResponse)
                .orElse(null);
    }

    /**
     * Gets RSVP history for a resident.
     */
    @Override
    @Transactional(readOnly = true)
    public List<MeetingAttendanceResponse> getResidentAttendances(
            Integer residentId
    ) {

        Resident resident = findResidentById(residentId);
        requireResidentOwnership(resident);

        return meetingAttendanceRepository.findByResident(resident)
                .stream()
                .map(meetingMapper::toMeetingAttendanceResponse)
                .toList();
    }

    // ==========================================================================
    // 3. RSVP Count Operations
    // ==========================================================================

    @Override
    @Transactional(readOnly = true)
    public long countGoing(String meetingUuid) {
        return countByStatus(meetingUuid, AttendanceStatus.GOING);
    }

    @Override
    @Transactional(readOnly = true)
    public long countNotGoing(String meetingUuid) {
        return countByStatus(meetingUuid, AttendanceStatus.NOT_GOING);
    }

    @Override
    @Transactional(readOnly = true)
    public long countMaybe(String meetingUuid) {
        return countByStatus(meetingUuid, AttendanceStatus.MAYBE);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPending(String meetingUuid) {
        return countByStatus(meetingUuid, AttendanceStatus.PENDING);
    }

    // ==========================================================================
    // 4. Helper Methods
    // ==========================================================================

    /**
     * Finds meeting by public UUID.
     */
    private Meeting findMeetingByUuid(String meetingUuid) {

        if (meetingUuid == null || meetingUuid.isBlank()) {
            throw new MeetingException("Meeting UUID is required.");
        }

        return meetingRepository.findByMeetingUuid(meetingUuid.trim())
                .orElseThrow(() ->
                        new MeetingNotFoundException(meetingUuid));
    }

    /**
     * Finds resident by ID.
     */
    private Resident findResidentById(Integer residentId) {

        if (residentId == null) {
            throw new ResidentException("Resident ID is required.");
        }

        return residentRepository.findById(residentId)
                .orElseThrow(() ->
                        new ResidentException(
                                "Resident not found with ID: " + residentId
                        ));
    }

    private void requireResidentOwnership(Resident resident) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new MeetingException("Authenticated user not found.");
        }
        boolean manager = authentication.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ADMIN") || authority.getAuthority().equals("SECRETARY")
                        || authority.getAuthority().equals("ROLE_ADMIN") || authority.getAuthority().equals("ROLE_SECRETARY"));
        if (manager) return;
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new MeetingException("Authenticated user not found."));
        if (resident.getUser() == null || !resident.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new MeetingException("You can access only your own RSVP records.");
        }
    }

    /**
     * Finds RSVP record for the meeting and resident.
     */
    private MeetingAttendance findAttendance(
            Meeting meeting,
            Resident resident
    ) {

        return meetingAttendanceRepository
                .findByMeetingAndResident(meeting, resident)
                .orElseThrow(() ->
                        new MeetingException(
                                "RSVP record not found for resident ID "
                                        + resident.getResidentId()
                                        + " and meeting "
                                        + meeting.getMeetingUuid()
                        ));
    }

    /**
     * Validates request object before applying RSVP changes.
     */
    private void validateAttendanceRequest(
            MeetingAttendanceRequest request
    ) {

        if (request == null) {
            throw new MeetingException("Attendance request is required.");
        }

        if (request.getAttendanceStatus() == null) {
            throw new MeetingException("Attendance status is required.");
        }
    }

    /**
     * Validates whether a meeting can accept RSVP changes.
     */
    private void validateMeetingAllowsAttendance(
            Meeting meeting,
            String message
    ) {

        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new MeetingException(
                    message + " Meeting is cancelled."
            );
        }

        if (meeting.getStatus() == MeetingStatus.COMPLETED) {
            throw new MeetingException(
                    message + " Meeting is completed."
            );
        }
    }

    /**
     * Counts RSVP records by attendance status.
     */
    private long countByStatus(
            String meetingUuid,
            AttendanceStatus attendanceStatus
    ) {

        Meeting meeting = findMeetingByUuid(meetingUuid);

        return meetingAttendanceRepository
                .countByMeetingAndAttendanceStatus(
                        meeting,
                        attendanceStatus
                );
    }
}
