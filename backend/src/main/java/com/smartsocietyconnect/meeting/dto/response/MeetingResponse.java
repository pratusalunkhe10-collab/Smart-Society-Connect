package com.smartsocietyconnect.meeting.dto.response;

import java.time.LocalDate;
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
 * Response DTO for listing society meetings.
 *
 * <p>This DTO contains only summary-level meeting information and is intended
 * for list screens, search results and upcoming meeting cards.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingResponse {

    // ==========================================================================
    // 1. Meeting Identity
    // ==========================================================================

    /**
     * Public meeting identifier used by APIs.
     */
    private String meetingUuid;

    // ==========================================================================
    // 2. Meeting Summary
    // ==========================================================================

    private String title;
    private String agenda;
    private MeetingType meetingType;
    private long attendeeCount;

    private LocalDate meetingDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String venue;
    private MeetingMode meetingMode;
    private String meetingLink;

    private MeetingStatus status;
    private MeetingAudience audience;
}
