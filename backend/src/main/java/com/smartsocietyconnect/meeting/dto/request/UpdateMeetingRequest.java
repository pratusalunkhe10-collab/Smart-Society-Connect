package com.smartsocietyconnect.meeting.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.smartsocietyconnect.meeting.enums.MeetingAudience;
import com.smartsocietyconnect.meeting.enums.MeetingType;
import com.smartsocietyconnect.meeting.enums.MeetingMode;

/**
 * Request DTO for updating an existing society meeting.
 *
 * <p>This DTO is used for full meeting updates. All main meeting fields are
 * required because this behaves like a complete update operation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMeetingRequest {

    // ==========================================================================
    // 1. Meeting Details
    // ==========================================================================

    @NotBlank(message = "Meeting title is required.")
    @Size(max = 100, message = "Title cannot exceed 100 characters.")
    private String title;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters.")
    private String description;

    @Size(max = 5000, message = "Agenda cannot exceed 5000 characters.")
    private String agenda;

    private MeetingType meetingType;

    // ==========================================================================
    // 2. Schedule Details
    // ==========================================================================

    @NotNull(message = "Meeting date is required.")
    @FutureOrPresent(message = "Meeting date cannot be in the past.")
    private LocalDate meetingDate;

    @NotNull(message = "Start time is required.")
    private LocalTime startTime;

    @NotNull(message = "End time is required.")
    private LocalTime endTime;

    // ==========================================================================
    // 3. Venue Details
    // ==========================================================================

    @NotBlank(message = "Venue is required.")
    @Size(max = 150, message = "Venue cannot exceed 150 characters.")
    private String venue;

    private MeetingMode meetingMode;

    @Size(max = 500, message = "Meeting link cannot exceed 500 characters.")
    private String meetingLink;

    private MeetingAudience audience;
}
