package com.smartsocietyconnect.complaint.dto.request;

import com.smartsocietyconnect.complaint.enums.ComplaintCategory;
import com.smartsocietyconnect.complaint.enums.ComplaintPriority;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO used to update complaint details.
 *
 * <p>All fields are optional to support partial updates. The service layer
 * should apply only non-null values.
 *
 * <p>Status changes should be done using {@link UpdateComplaintStatusRequest},
 * not this DTO.
 */
@Getter
@Setter
public class UpdateComplaintRequest {

    /**
     * Updated complaint title.
     */
    @Size(max = 100, message = "Title cannot exceed 100 characters")
    private String title;

    /**
     * Updated complaint description.
     */
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    /**
     * Updated complaint category.
     */
    private ComplaintCategory category;

    /**
     * Updated complaint priority.
     */
    private ComplaintPriority priority;

    /**
     * Updated attachment path or URL.
     */
    @Size(max = 255, message = "Attachment path cannot exceed 255 characters")
    private String attachment;
}