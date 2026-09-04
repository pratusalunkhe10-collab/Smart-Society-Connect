package com.smartsocietyconnect.complaint.dto.request;

import com.smartsocietyconnect.complaint.enums.ComplaintStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO used to update complaint workflow status.
 *
 * <p>This DTO is used by admin/staff workflows to move complaints through
 * statuses such as {@code IN_PROGRESS}, {@code RESOLVED}, {@code CLOSED},
 * or {@code REJECTED}.
 */
@Getter
@Setter
public class UpdateComplaintStatusRequest {

    /**
     * New complaint status.
     */
    @NotNull(message = "Status is required")
    private ComplaintStatus status;

    /**
     * Staff/team/person assigned to resolve the complaint.
     */
    @Size(max = 100, message = "Assigned To cannot exceed 100 characters")
    private String assignedTo;

    /**
     * Resolution, rejection, or closing remarks.
     */
    @Size(max = 1000, message = "Resolution remarks cannot exceed 1000 characters")
    private String resolutionRemarks;
}