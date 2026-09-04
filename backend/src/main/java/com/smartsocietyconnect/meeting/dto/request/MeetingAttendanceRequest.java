package com.smartsocietyconnect.meeting.dto.request;

import com.smartsocietyconnect.meeting.enums.AttendanceStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for submitting or updating a resident RSVP.
 *
 * <p>The same DTO can be used for first-time RSVP submission and later RSVP
 * updates.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingAttendanceRequest {

    // ==========================================================================
    // 1. RSVP Details
    // ==========================================================================

    @NotNull(message = "Attendance status is required.")
    private AttendanceStatus attendanceStatus;

    @Size(max = 255, message = "Remarks cannot exceed 255 characters.")
    private String remarks;
}