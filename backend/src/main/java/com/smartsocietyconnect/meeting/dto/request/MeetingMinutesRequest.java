package com.smartsocietyconnect.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for creating or updating Minutes of Meeting.
 *
 * <p>The uploaded file field stores the saved file name or file path returned
 * by the file storage service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingMinutesRequest {

    // ==========================================================================
    // 1. Minutes Details
    // ==========================================================================

    @NotBlank(message = "Meeting summary is required.")
    @Size(max = 10000, message = "Summary cannot exceed 10000 characters.")
    private String summary;

    @Size(max = 255, message = "Uploaded file path cannot exceed 255 characters.")
    private String uploadedFile;
}