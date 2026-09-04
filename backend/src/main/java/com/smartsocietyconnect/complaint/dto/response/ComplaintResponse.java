package com.smartsocietyconnect.complaint.dto.response;

import java.time.LocalDateTime;

import com.smartsocietyconnect.complaint.enums.ComplaintCategory;
import com.smartsocietyconnect.complaint.enums.ComplaintPriority;
import com.smartsocietyconnect.complaint.enums.ComplaintStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO returned after complaint-related operations.
 *
 * <p>Represents a flattened, read-only complaint view for API clients.
 *
 * <p>This DTO may combine fields from:
 * <ul>
 *     <li>{@code complaints}</li>
 *     <li>{@code residents}</li>
 *     <li>{@code users}, through resident user details</li>
 * </ul>
 */
@Getter
@Builder
public class ComplaintResponse {

    /**
     * Complaint primary key.
     */
    private Integer complaintId;

    /**
     * Public complaint tracking number.
     */
    private String complaintNumber;

    /**
     * Resident ID who raised the complaint.
     */
    private Integer residentId;

    /**
     * Resident full name.
     */
    private String residentName;

    private String title;

    private String description;

    private ComplaintCategory category;

    private ComplaintPriority priority;

    private ComplaintStatus status;

    private String attachment;

    private String assignedTo;

    private String resolutionRemarks;

    private LocalDateTime resolvedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}