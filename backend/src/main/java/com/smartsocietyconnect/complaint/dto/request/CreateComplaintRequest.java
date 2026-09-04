package com.smartsocietyconnect.complaint.dto.request;

import com.smartsocietyconnect.complaint.enums.ComplaintCategory;
import com.smartsocietyconnect.complaint.enums.ComplaintPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO used to create a new complaint.
 *
 * <p>This DTO contains only client-supplied fields required when a resident
 * raises a complaint.
 *
 * <p>Fields such as {@code complaintNumber}, {@code status},
 * {@code resolutionRemarks}, {@code resolvedAt}, {@code createdAt}, and
 * {@code updatedAt} are intentionally excluded because they are controlled
 * by the service layer or database.
 */
@Getter
@Setter
public class CreateComplaintRequest {

    /**
     * ID of the resident raising the complaint.
     *
     * <p>References {@code residents.resident_id}.
     */
    @NotNull(message = "Resident ID is required")
    private Integer residentId;

    /**
     * Short complaint title.
     */
    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title cannot exceed 100 characters")
    private String title;

    /**
     * Detailed complaint description.
     *
     * <p>Database column is TEXT, but API validation keeps input controlled.
     */
    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    /**
     * Complaint category used for routing/filtering.
     */
    @NotNull(message = "Category is required")
    private ComplaintCategory category;

    /**
     * Complaint priority.
     *
     * <p>If omitted, defaults to {@code MEDIUM}.
     */
    private ComplaintPriority priority = ComplaintPriority.MEDIUM;

    /**
     * Optional attachment path or URL.
     */
    @Size(max = 255, message = "Attachment path cannot exceed 255 characters")
    private String attachment;
}