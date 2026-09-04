package com.smartsocietyconnect.meeting.mapper;

import org.springframework.stereotype.Component;

import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.meeting.dto.response.MeetingAttendanceResponse;
import com.smartsocietyconnect.meeting.dto.response.MeetingDetailsResponse;
import com.smartsocietyconnect.meeting.dto.response.MeetingMinutesResponse;
import com.smartsocietyconnect.meeting.dto.response.MeetingResponse;
import com.smartsocietyconnect.meeting.entity.Meeting;
import com.smartsocietyconnect.meeting.entity.MeetingAttendance;
import com.smartsocietyconnect.meeting.entity.MeetingMinutes;
import com.smartsocietyconnect.resident.entity.Resident;

/**
 * Mapper class for Meeting Module.
 *
 * <p>Converts Meeting Module entities into response DTOs.
 *
 * <p>This mapper does not fetch database records. Database lookups belong to
 * the service layer.
 */
@Component
public class MeetingMapper {

    // ==========================================================================
    // 1. Meeting Mapping
    // ==========================================================================

    /**
     * Converts {@link Meeting} entity to summary response DTO.
     *
     * @param meeting meeting entity
     * @return meeting summary response
     */
    public MeetingResponse toMeetingResponse(Meeting meeting) {

        if (meeting == null) {
            return null;
        }

        return MeetingResponse.builder()
                .meetingUuid(meeting.getMeetingUuid())
                .title(meeting.getTitle())
                .agenda(meeting.getAgenda())
                .meetingType(meeting.getMeetingType())
                .meetingDate(meeting.getMeetingDate())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .venue(meeting.getVenue())
                .meetingMode(meeting.getMeetingMode())
                .meetingLink(meeting.getMeetingLink())
                .status(meeting.getStatus())
                .audience(meeting.getAudience())
                .build();
    }

    /**
     * Converts {@link Meeting} entity to detailed response DTO.
     *
     * @param meeting meeting entity
     * @return detailed meeting response
     */
    public MeetingDetailsResponse toMeetingDetailsResponse(Meeting meeting) {

        if (meeting == null) {
            return null;
        }

        return MeetingDetailsResponse.builder()
                .meetingUuid(meeting.getMeetingUuid())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .agenda(meeting.getAgenda())
                .meetingType(meeting.getMeetingType())
                .meetingDate(meeting.getMeetingDate())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .venue(meeting.getVenue())
                .meetingMode(meeting.getMeetingMode())
                .meetingLink(meeting.getMeetingLink())
                .status(meeting.getStatus())
                .audience(meeting.getAudience())
                .createdBy(formatUserName(meeting.getCreatedBy()))
                .createdAt(meeting.getCreatedAt())
                .updatedAt(meeting.getUpdatedAt())
                .build();
    }

    // ==========================================================================
    // 2. Attendance Mapping
    // ==========================================================================

    /**
     * Converts {@link MeetingAttendance} entity to response DTO.
     *
     * @param attendance meeting attendance entity
     * @return meeting attendance response
     */
    public MeetingAttendanceResponse toMeetingAttendanceResponse(
            MeetingAttendance attendance
    ) {

        if (attendance == null) {
            return null;
        }

        Resident resident = attendance.getResident();

        return MeetingAttendanceResponse.builder()
                .residentId(resident != null ? resident.getResidentId() : null)
                .residentName(formatResidentName(resident))
                .attendanceStatus(attendance.getAttendanceStatus())
                .remarks(attendance.getRemarks())
                .respondedAt(attendance.getRespondedAt())
                .build();
    }

    // ==========================================================================
    // 3. Minutes Mapping
    // ==========================================================================

    /**
     * Converts {@link MeetingMinutes} entity to response DTO.
     *
     * @param minutes meeting minutes entity
     * @return meeting minutes response
     */
    public MeetingMinutesResponse toMeetingMinutesResponse(
            MeetingMinutes minutes
    ) {

        if (minutes == null) {
            return null;
        }

        return MeetingMinutesResponse.builder()
                .meetingUuid(
                        minutes.getMeeting() != null
                                ? minutes.getMeeting().getMeetingUuid()
                                : null
                )
                .summary(minutes.getSummary())
                .uploadedFile(minutes.getUploadedFile())
                .preparedBy(formatUserName(minutes.getPreparedBy()))
                .createdAt(minutes.getCreatedAt())
                .updatedAt(minutes.getUpdatedAt())
                .build();
    }

    // ==========================================================================
    // 4. Helper Methods
    // ==========================================================================

    /**
     * Formats a user's full name safely.
     *
     * @param user user entity
     * @return formatted full name
     */
    private String formatUserName(User user) {

        if (user == null) {
            return null;
        }

        return joinName(
                user.getFirstName(),
                user.getLastName()
        );
    }

    /**
     * Formats resident name using the linked user profile.
     *
     * @param resident resident entity
     * @return formatted resident name
     */
    private String formatResidentName(Resident resident) {

        if (resident == null || resident.getUser() == null) {
            return null;
        }

        return joinName(
                resident.getUser().getFirstName(),
                resident.getUser().getLastName()
        );
    }

    /**
     * Joins first and last name without producing extra spaces.
     *
     * @param firstName first name
     * @param lastName last name
     * @return full name
     */
    private String joinName(String firstName, String lastName) {

        String safeFirstName = firstName == null ? "" : firstName.trim();
        String safeLastName = lastName == null ? "" : lastName.trim();

        String fullName = (safeFirstName + " " + safeLastName).trim();

        return fullName.isBlank() ? null : fullName;
    }
}
