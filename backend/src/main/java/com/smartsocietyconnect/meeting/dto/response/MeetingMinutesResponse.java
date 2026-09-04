package com.smartsocietyconnect.meeting.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for Minutes of Meeting.
 *
 * <p>This DTO returns the meeting summary, optional uploaded file reference,
 * preparer name and audit timestamps.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingMinutesResponse {

    // ==========================================================================
    // 1. Meeting Identity
    // ==========================================================================

    private String meetingUuid;

    // ==========================================================================
    // 2. Minutes Details
    // ==========================================================================

    private String summary;

    private String uploadedFile;

    private String preparedBy;

    // ==========================================================================
    // 3. Audit Details
    // ==========================================================================

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}