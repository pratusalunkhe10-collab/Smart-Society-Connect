package com.smartsocietyconnect.meeting.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.smartsocietyconnect.meeting.enums.MeetingStatus;
import com.smartsocietyconnect.meeting.enums.MeetingAudience;
import com.smartsocietyconnect.meeting.enums.MeetingType;
import com.smartsocietyconnect.meeting.enums.MeetingMode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Detailed response DTO for a society meeting.
 *
 * <p>This DTO is used on the meeting details page and contains full meeting
 * information including description, agenda, creator and audit timestamps.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingDetailsResponse {

    // ==========================================================================
    // 1. Meeting Identity
    // ==========================================================================

    /**
     * Public meeting identifier used by APIs.
     */
    private String meetingUuid;

    // ==========================================================================
    // 2. Meeting Details
    // ==========================================================================

    private String title;

    private String description;

    private String agenda;
    private MeetingType meetingType;

    private LocalDate meetingDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String venue;
    private MeetingMode meetingMode;
    private String meetingLink;

    private MeetingStatus status;
    private MeetingAudience audience;

    // ==========================================================================
    // 3. Creator And Audit Details
    // ==========================================================================

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
