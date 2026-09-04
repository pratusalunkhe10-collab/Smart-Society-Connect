package com.smartsocietyconnect.meeting.dto.response;

import java.time.LocalDateTime;

import com.smartsocietyconnect.meeting.enums.AttendanceStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for a resident's meeting RSVP.
 *
 * <p>This DTO is used when returning attendees of a meeting or the RSVP history
 * of a resident.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingAttendanceResponse {

    // ==========================================================================
    // 1. Resident Details
    // ==========================================================================

    private Integer residentId;

    private String residentName;

    // ==========================================================================
    // 2. RSVP Details
    // ==========================================================================

    private AttendanceStatus attendanceStatus;

    private String remarks;

    private LocalDateTime respondedAt;
}