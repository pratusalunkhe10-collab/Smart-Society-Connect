package com.smartsocietyconnect.complaint.service;

import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.smartsocietyconnect.complaint.dto.request.CreateComplaintRequest;
import com.smartsocietyconnect.complaint.dto.request.UpdateComplaintRequest;
import com.smartsocietyconnect.complaint.dto.request.UpdateComplaintStatusRequest;
import com.smartsocietyconnect.complaint.dto.response.ComplaintResponse;
import com.smartsocietyconnect.complaint.enums.ComplaintCategory;
import com.smartsocietyconnect.complaint.enums.ComplaintPriority;
import com.smartsocietyconnect.complaint.enums.ComplaintStatus;

/**
 * Service contract for complaint management operations.
 *
 * <p>Defines business logic for complaint creation, update, deletion,
 * status workflow changes, and complaint lookup/filtering.
 *
 * <p>Implementations should enforce complaint workflow rules such as:
 * <ul>
 *     <li>New complaints should start with {@link ComplaintStatus#OPEN}.</li>
 *     <li>Resolved complaints should set {@code resolvedAt}.</li>
 *     <li>Closed complaints should not be edited unless explicitly allowed.</li>
 *     <li>Complaint numbers must be unique.</li>
 * </ul>
 */
public interface ComplaintService {

    // ==========================================================================
    // 1. Create Complaint
    // ==========================================================================

    /**
     * Creates a new complaint.
     *
     * <p>The implementation should validate the resident, generate a unique
     * complaint number, apply default priority/status values, and persist
     * the complaint.
     *
     * @param request complaint creation request
     * @return created complaint response
     */
    ComplaintResponse createComplaint(CreateComplaintRequest request);

    // ==========================================================================
    // 2. Update Complaint Details
    // ==========================================================================

    /**
     * Updates complaint details.
     *
     * <p>This method is intended for editable complaint fields such as title,
     * description, category, priority, and attachment. Status changes should
     * be handled by {@link #updateComplaintStatus(Integer, UpdateComplaintStatusRequest)}.
     *
     * @param complaintId complaint primary key
     * @param request complaint update request
     * @return updated complaint response
     */
    ComplaintResponse updateComplaint(
            Integer complaintId,
            UpdateComplaintRequest request
    );

    ComplaintResponse uploadAttachment(Integer complaintId, MultipartFile file);

    Resource loadAttachment(Integer complaintId);

    // ==========================================================================
    // 3. Delete Complaint
    // ==========================================================================

    /**
     * Deletes a complaint.
     *
     * <p>Implementations may block deletion for resolved/closed complaints
     * depending on audit/history requirements.
     *
     * @param complaintId complaint primary key
     */
    void deleteComplaint(Integer complaintId);

    // ==========================================================================
    // 4. Read Operations
    // ==========================================================================

    /**
     * Fetches a complaint by ID.
     *
     * @param complaintId complaint primary key
     * @return complaint response
     */
    ComplaintResponse getComplaintById(Integer complaintId);

    /**
     * Fetches a complaint by public complaint number.
     *
     * @param complaintNumber unique complaint tracking number
     * @return complaint response
     */
    ComplaintResponse getComplaintByNumber(String complaintNumber);

    /**
     * Fetches all complaints.
     *
     * @return list of complaints
     */
    List<ComplaintResponse> getAllComplaints();

    /**
     * Fetches all complaints raised by a resident.
     *
     * @param residentId resident primary key
     * @return complaints raised by the resident
     */
    List<ComplaintResponse> getComplaintsByResident(Integer residentId);

    /**
     * Fetches complaints by status.
     *
     * @param status complaint status
     * @return complaints matching the status
     */
    List<ComplaintResponse> getComplaintsByStatus(ComplaintStatus status);

    /**
     * Fetches complaints by priority.
     *
     * @param priority complaint priority
     * @return complaints matching the priority
     */
    List<ComplaintResponse> getComplaintsByPriority(ComplaintPriority priority);

    /**
     * Fetches complaints by category.
     *
     * @param category complaint category
     * @return complaints matching the category
     */
    List<ComplaintResponse> getComplaintsByCategory(ComplaintCategory category);

    // ==========================================================================
    // 5. Status Workflow
    // ==========================================================================

    /**
     * Updates complaint status and related workflow fields.
     *
     * <p>For example, when status becomes {@link ComplaintStatus#RESOLVED},
     * the implementation should set {@code resolvedAt}.
     *
     * @param complaintId complaint primary key
     * @param request status update request
     * @return updated complaint response
     */
    ComplaintResponse updateComplaintStatus(
            Integer complaintId,
            UpdateComplaintStatusRequest request
    );
}
