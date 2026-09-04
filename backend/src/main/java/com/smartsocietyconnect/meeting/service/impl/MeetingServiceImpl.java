package com.smartsocietyconnect.meeting.service.impl;

import java.time.LocalDate;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.auth.repository.UserRepository;
import com.smartsocietyconnect.auth.repository.RoleRepository;
import com.smartsocietyconnect.auth.repository.UserRoleRepository;
import com.smartsocietyconnect.meeting.dto.request.CreateMeetingRequest;
import com.smartsocietyconnect.meeting.dto.request.MeetingMinutesRequest;
import com.smartsocietyconnect.meeting.dto.request.UpdateMeetingRequest;
import com.smartsocietyconnect.meeting.dto.response.MeetingDetailsResponse;
import com.smartsocietyconnect.meeting.dto.response.MeetingMinutesResponse;
import com.smartsocietyconnect.meeting.dto.response.MeetingResponse;
import com.smartsocietyconnect.meeting.entity.Meeting;
import com.smartsocietyconnect.meeting.entity.MeetingMinutes;
import com.smartsocietyconnect.meeting.enums.MeetingStatus;
import com.smartsocietyconnect.meeting.enums.MeetingAudience;
import com.smartsocietyconnect.meeting.enums.MeetingType;
import com.smartsocietyconnect.meeting.enums.MeetingMode;
import com.smartsocietyconnect.meeting.exception.DuplicateMeetingException;
import com.smartsocietyconnect.meeting.exception.InvalidMeetingStatusException;
import com.smartsocietyconnect.meeting.exception.MeetingAlreadyCompletedException;
import com.smartsocietyconnect.meeting.exception.MeetingCancelledException;
import com.smartsocietyconnect.meeting.exception.MeetingException;
import com.smartsocietyconnect.meeting.exception.MeetingNotFoundException;
import com.smartsocietyconnect.meeting.mapper.MeetingMapper;
import com.smartsocietyconnect.meeting.repository.MeetingAttendanceRepository;
import com.smartsocietyconnect.meeting.repository.MeetingMinutesRepository;
import com.smartsocietyconnect.meeting.repository.MeetingRepository;
import com.smartsocietyconnect.meeting.service.MeetingService;
import com.smartsocietyconnect.notification.service.NotificationService;
import com.smartsocietyconnect.resident.repository.ResidentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for Meeting Module.
 *
 * <p>Handles meeting creation, updates, status transitions, cancellation,
 * deletion, read operations and Minutes of Meeting management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MeetingServiceImpl implements MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingAttendanceRepository meetingAttendanceRepository;
    private final MeetingMinutesRepository meetingMinutesRepository;
    private final UserRepository userRepository;
    private final ResidentRepository residentRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final NotificationService notificationService;
    private final MeetingMapper meetingMapper;

    // ==========================================================================
    // 1. Meeting Write Operations
    // ==========================================================================

    /**
     * Creates a new society meeting.
     */
    @Override
    public MeetingResponse createMeeting(CreateMeetingRequest request) {

        validateSchedule(
                request.getMeetingDate(),
                request.getStartTime(),
                request.getEndTime()
        );

        validateDuplicateMeeting(request);
        validateMeetingAccess(request.getMeetingMode(), request.getMeetingLink());

        User createdBy = getAuthenticatedUser();

        Meeting meeting = Meeting.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .agenda(request.getAgenda())
                .meetingType(request.getMeetingType() == null ? MeetingType.GENERAL_BODY : request.getMeetingType())
                .meetingDate(request.getMeetingDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .venue(request.getVenue().trim())
                .meetingMode(request.getMeetingMode() == null ? MeetingMode.IN_PERSON : request.getMeetingMode())
                .meetingLink(normalizeMeetingLink(request.getMeetingLink()))
                .status(MeetingStatus.SCHEDULED)
                .audience(request.getAudience() == null ? MeetingAudience.ALL_RESIDENTS : request.getAudience())
                .createdBy(createdBy)
                .build();

        Meeting savedMeeting = meetingRepository.save(meeting);
        notifyMeetingAudience(savedMeeting);

        log.info(
                "Meeting created successfully. meetingUuid={}, createdBy={}",
                savedMeeting.getMeetingUuid(),
                createdBy.getEmail()
        );

        return toMeetingResponse(savedMeeting);
    }

    /**
     * Updates an existing meeting.
     */
    @Override
    public MeetingResponse updateMeeting(
            String meetingUuid,
            UpdateMeetingRequest request
    ) {

        Meeting meeting = findMeetingByUuid(meetingUuid);

        if (meeting.getStatus() == MeetingStatus.COMPLETED) {
            throw new MeetingAlreadyCompletedException(meetingUuid);
        }

        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new MeetingCancelledException(meetingUuid);
        }

        validateSchedule(
                request.getMeetingDate(),
                request.getStartTime(),
                request.getEndTime()
        );
        validateMeetingAccess(request.getMeetingMode(), request.getMeetingLink());

        meeting.setTitle(request.getTitle().trim());
        meeting.setDescription(request.getDescription());
        meeting.setAgenda(request.getAgenda());
        if (request.getMeetingType() != null) {
            meeting.setMeetingType(request.getMeetingType());
        }
        meeting.setMeetingDate(request.getMeetingDate());
        meeting.setStartTime(request.getStartTime());
        meeting.setEndTime(request.getEndTime());
        meeting.setVenue(request.getVenue().trim());
        meeting.setMeetingMode(request.getMeetingMode() == null ? MeetingMode.IN_PERSON : request.getMeetingMode());
        meeting.setMeetingLink(normalizeMeetingLink(request.getMeetingLink()));
        if (request.getAudience() != null) {
            meeting.setAudience(request.getAudience());
        }

        Meeting updatedMeeting = meetingRepository.save(meeting);
        notifyMeetingAudience(updatedMeeting, "updated");

        log.info("Meeting updated successfully. meetingUuid={}", meetingUuid);

        return toMeetingResponse(updatedMeeting);
    }

    /**
     * Updates meeting lifecycle status.
     */
    @Override
    public MeetingResponse updateMeetingStatus(
            String meetingUuid,
            MeetingStatus requestedStatus
    ) {

        if (requestedStatus == null) {
            throw new InvalidMeetingStatusException(
                    "Requested meeting status is required."
            );
        }

        Meeting meeting = findMeetingByUuid(meetingUuid);

        validateStatusTransition(
                meeting.getStatus(),
                requestedStatus
        );

        meeting.setStatus(requestedStatus);

        Meeting updatedMeeting = meetingRepository.save(meeting);

        if (requestedStatus == MeetingStatus.ONGOING) {
            notifyMeetingAudience(updatedMeeting, "started");
        } else if (requestedStatus == MeetingStatus.COMPLETED) {
            notifyMeetingAudience(updatedMeeting, "completed");
        }

        log.info(
                "Meeting status updated. meetingUuid={}, status={}",
                meetingUuid,
                requestedStatus
        );

        return toMeetingResponse(updatedMeeting);
    }

    /**
     * Cancels a scheduled or ongoing meeting.
     */
    @Override
    public MeetingResponse cancelMeeting(String meetingUuid) {

        Meeting meeting = findMeetingByUuid(meetingUuid);

        if (meeting.getStatus() == MeetingStatus.COMPLETED) {
            throw new MeetingAlreadyCompletedException(meetingUuid);
        }

        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new MeetingCancelledException(meetingUuid);
        }

        meeting.setStatus(MeetingStatus.CANCELLED);

        Meeting cancelledMeeting = meetingRepository.save(meeting);
        notifyMeetingAudience(cancelledMeeting, "cancelled");

        log.info("Meeting cancelled successfully. meetingUuid={}", meetingUuid);

        return toMeetingResponse(cancelledMeeting);
    }

    /**
     * Deletes a meeting.
     *
     * <p>Deletion is blocked if attendees or minutes already exist, because
     * those records are historical and should not be accidentally lost.
     */
    @Override
    public void deleteMeeting(String meetingUuid) {

        Meeting meeting = findMeetingByUuid(meetingUuid);

        if (meetingMinutesRepository.existsByMeeting(meeting)) {
            throw new MeetingException(
                    "Meeting cannot be deleted because meeting minutes already exist."
            );
        }

        if (meetingAttendanceRepository.countByMeeting(meeting) > 0) {
            throw new MeetingException(
                    "Meeting cannot be deleted because attendee records already exist."
            );
        }

        meetingRepository.delete(meeting);

        log.info("Meeting deleted successfully. meetingUuid={}", meetingUuid);
    }

    // ==========================================================================
    // 2. Meeting Read Operations
    // ==========================================================================

    /**
     * Gets meeting details by UUID.
     */
    @Override
    @Transactional(readOnly = true)
    public MeetingDetailsResponse getMeetingByUuid(String meetingUuid) {

        Meeting meeting = findMeetingByUuid(meetingUuid);

        requireVisibleToCurrentUser(meeting);
        MeetingDetailsResponse response = meetingMapper.toMeetingDetailsResponse(meeting);
        hideMeetingLinkIfUnauthorized(response, meeting);
        return response;
    }

    /**
     * Gets all meetings ordered by latest created first.
     */
    @Override
    @Transactional(readOnly = true)
    public List<MeetingResponse> getAllMeetings() {

        return visibleMeetings(meetingRepository.findAllByOrderByCreatedAtDesc())
                .stream()
                .map(this::toMeetingResponse)
                .toList();
    }

    /**
     * Gets upcoming meetings from today onward.
     */
    @Override
    @Transactional(readOnly = true)
    public List<MeetingResponse> getUpcomingMeetings() {

        return visibleMeetings(meetingRepository
                .findByMeetingDateGreaterThanEqualOrderByMeetingDateAsc(
                        LocalDate.now()
                ))
                .stream()
                .map(this::toMeetingResponse)
                .toList();
    }

    /**
     * Gets completed meetings.
     */
    @Override
    @Transactional(readOnly = true)
    public List<MeetingResponse> getCompletedMeetings() {

        return visibleMeetings(meetingRepository
                .findByStatusOrderByMeetingDateDesc(MeetingStatus.COMPLETED)
                )
                .stream()
                .map(this::toMeetingResponse)
                .toList();
    }

    /**
     * Gets meetings by status.
     */
    @Override
    @Transactional(readOnly = true)
    public List<MeetingResponse> getMeetingsByStatus(MeetingStatus status) {

        if (status == null) {
            throw new InvalidMeetingStatusException(
                    "Meeting status is required."
            );
        }

        return visibleMeetings(meetingRepository.findByStatus(status))
                .stream()
                .map(this::toMeetingResponse)
                .toList();
    }

    /**
     * Searches meetings by title keyword.
     */
    @Override
    @Transactional(readOnly = true)
    public List<MeetingResponse> searchMeetings(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            throw new MeetingException("Search keyword is required.");
        }

        return visibleMeetings(meetingRepository
                .findByTitleContainingIgnoreCase(keyword.trim())
                )
                .stream()
                .map(this::toMeetingResponse)
                .toList();
    }

    // ==========================================================================
    // 3. Meeting Minutes Operations
    // ==========================================================================

    /**
     * Creates or updates Minutes of Meeting.
     */
    @Override
    public MeetingMinutesResponse uploadMeetingMinutes(
            String meetingUuid,
            MeetingMinutesRequest request
    ) {

        Meeting meeting = findMeetingByUuid(meetingUuid);

        if (meeting.getStatus() != MeetingStatus.COMPLETED) {
            throw new InvalidMeetingStatusException(
                    "Meeting minutes can only be uploaded after the meeting is completed."
            );
        }

        User preparedBy = getAuthenticatedUser();

        MeetingMinutes meetingMinutes = meetingMinutesRepository
                .findByMeeting(meeting)
                .orElse(MeetingMinutes.builder()
                        .meeting(meeting)
                        .build());

        meetingMinutes.setSummary(request.getSummary().trim());
        meetingMinutes.setUploadedFile(request.getUploadedFile());
        meetingMinutes.setPreparedBy(preparedBy);

        MeetingMinutes savedMinutes =
                meetingMinutesRepository.save(meetingMinutes);

        log.info(
                "Meeting minutes saved successfully. meetingUuid={}, preparedBy={}",
                meetingUuid,
                preparedBy.getEmail()
        );

        return meetingMapper.toMeetingMinutesResponse(savedMinutes);
    }

    /**
     * Gets Minutes of Meeting by meeting UUID.
     */
    @Override
    @Transactional(readOnly = true)
    public MeetingMinutesResponse getMeetingMinutes(String meetingUuid) {

        Meeting meeting = findMeetingByUuid(meetingUuid);

        MeetingMinutes meetingMinutes = meetingMinutesRepository
                .findByMeeting(meeting)
                .orElseThrow(() -> new MeetingException(
                        "Meeting minutes not found for meeting: " + meetingUuid
                ));

        return meetingMapper.toMeetingMinutesResponse(meetingMinutes);
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

        return meetingRepository
                .findByMeetingUuid(meetingUuid.trim())
                .orElseThrow(() ->
                        new MeetingNotFoundException(meetingUuid));
    }

    /**
     * Prevents duplicate meetings with same title, date and start time.
     */
    private void validateDuplicateMeeting(CreateMeetingRequest request) {

        boolean exists = meetingRepository
                .existsByTitleIgnoreCaseAndMeetingDateAndStartTime(
                        request.getTitle().trim(),
                        request.getMeetingDate(),
                        request.getStartTime()
                );

        if (exists) {
            throw new DuplicateMeetingException(
                    "A meeting with the same title, date and start time already exists."
            );
        }
    }

    /**
     * Validates meeting schedule.
     */
    private void validateSchedule(
            LocalDate meetingDate,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime
    ) {

        if (meetingDate == null || startTime == null || endTime == null) {
            throw new MeetingException(
                    "Meeting date, start time and end time are required."
            );
        }

        if (!endTime.isAfter(startTime)) {
            throw new MeetingException(
                    "Meeting end time must be after start time."
            );
        }
    }

    /**
     * Validates allowed meeting status transitions.
     */
    private void validateStatusTransition(
            MeetingStatus currentStatus,
            MeetingStatus requestedStatus
    ) {

        if (currentStatus == requestedStatus) {
            return;
        }

        switch (currentStatus) {

            case SCHEDULED -> {

                if (requestedStatus != MeetingStatus.ONGOING
                        && requestedStatus != MeetingStatus.CANCELLED) {

                    throw new InvalidMeetingStatusException(
                            currentStatus,
                            requestedStatus
                    );
                }
            }

            case ONGOING -> {

                if (requestedStatus != MeetingStatus.COMPLETED
                        && requestedStatus != MeetingStatus.CANCELLED) {

                    throw new InvalidMeetingStatusException(
                            currentStatus,
                            requestedStatus
                    );
                }
            }

            case COMPLETED -> throw new InvalidMeetingStatusException(
                    "Completed meetings cannot change status."
            );

            case CANCELLED -> throw new InvalidMeetingStatusException(
                    "Cancelled meetings cannot change status."
            );
        }
    }

    /**
     * Gets the currently authenticated user from Spring Security.
     *
     * <p>Your project stores the authenticated username as email, so this method
     * resolves the full User entity using UserRepository.
     */
    private User getAuthenticatedUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new MeetingException("Authenticated user not found.");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new MeetingException(
                                "Authenticated user not found with email: "
                                        + email
                        ));
    }

    private MeetingResponse toMeetingResponse(Meeting meeting) {
        MeetingResponse response = meetingMapper.toMeetingResponse(meeting);
        response.setAttendeeCount(meetingAttendanceRepository.countByMeeting(meeting));
        if (!canCurrentUserAccessMeetingLink(meeting)) {
            response.setMeetingLink(null);
        }
        return response;
    }

    private void hideMeetingLinkIfUnauthorized(MeetingDetailsResponse response, Meeting meeting) {
        if (!canCurrentUserAccessMeetingLink(meeting)) {
            response.setMeetingLink(null);
        }
    }

    private boolean canCurrentUserAccessMeetingLink(Meeting meeting) {
        if (isCommitteeUser()) {
            return true;
        }
        return meeting.getAudience() == MeetingAudience.ALL_RESIDENTS && isResidentUser();
    }

    private boolean isResidentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("RESIDENT")
                        || authority.getAuthority().equals("ROLE_RESIDENT"));
    }

    private void validateMeetingAccess(MeetingMode mode, String link) {
        MeetingMode safeMode = mode == null ? MeetingMode.IN_PERSON : mode;
        String safeLink = normalizeMeetingLink(link);
        if ((safeMode == MeetingMode.ONLINE || safeMode == MeetingMode.HYBRID) && safeLink == null) {
            throw new MeetingException("An online meeting link is required for online or hybrid meetings.");
        }
        if (safeLink != null) {
            try {
                URI uri = URI.create(safeLink);
                if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                    throw new MeetingException("Meeting link must be a valid HTTPS URL.");
                }
            } catch (IllegalArgumentException exception) {
                throw new MeetingException("Meeting link must be a valid HTTPS URL.");
            }
        }
    }

    private String normalizeMeetingLink(String link) {
        return link == null || link.isBlank() ? null : link.trim();
    }

    private List<Meeting> visibleMeetings(List<Meeting> meetings) {
        if (isCommitteeUser()) return meetings;
        return meetings.stream().filter(meeting -> meeting.getAudience() != MeetingAudience.COMMITTEE_ONLY).toList();
    }

    private void requireVisibleToCurrentUser(Meeting meeting) {
        if (meeting.getAudience() == MeetingAudience.COMMITTEE_ONLY && !isCommitteeUser()) {
            throw new MeetingNotFoundException(meeting.getMeetingUuid());
        }
    }

    private boolean isCommitteeUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ADMIN") || authority.getAuthority().equals("SECRETARY") || authority.getAuthority().equals("ROLE_ADMIN") || authority.getAuthority().equals("ROLE_SECRETARY"));
    }

    private void notifyMeetingAudience(Meeting meeting) {
        notifyMeetingAudience(meeting, "scheduled");
    }

    private void notifyMeetingAudience(Meeting meeting, String event) {
        Set<User> recipients;
        if (meeting.getAudience() == MeetingAudience.COMMITTEE_ONLY) {
            recipients = Set.of("ADMIN", "SECRETARY").stream()
                    .flatMap(roleName -> roleRepository.findByRoleName(roleName).stream())
                    .flatMap(role -> userRoleRepository.findByRole(role).stream())
                    .map(userRole -> userRole.getUser())
                    .collect(Collectors.toSet());
        } else {
            recipients = residentRepository.findByMoveOutDateIsNull().stream()
                    .map(resident -> resident.getUser()).collect(Collectors.toSet());
        }
        String group = meeting.getAudience() == MeetingAudience.COMMITTEE_ONLY ? "Committee meeting" : "Society meeting";
        String title = group + " " + event + ": " + meeting.getTitle();
        String detail = switch (event) {
            case "cancelled" -> "This meeting has been cancelled.";
            case "started" -> "The meeting is now in progress at "
                    + meeting.getVenue() + ".";
            case "completed" -> "The meeting has concluded. Minutes will be available when published.";
            case "updated" -> "Meeting details were updated. Scheduled for "
                    + meeting.getMeetingDate() + " at " + meeting.getVenue() + ".";
            default -> "Scheduled for " + meeting.getMeetingDate()
                    + " at " + meeting.getVenue() + ".";
        };
        notificationService.notifyUsers(recipients, "MEETING", title, detail, "/app/meetings");
    }
}
