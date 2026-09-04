package com.smartsocietyconnect.visitor.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import com.smartsocietyconnect.visitor.dto.VisitorApprovalDTO;
import com.smartsocietyconnect.visitor.dto.VisitorCheckInDTO;
import com.smartsocietyconnect.visitor.dto.VisitorCheckOutDTO;
import com.smartsocietyconnect.visitor.dto.VisitorRequestDTO;
import com.smartsocietyconnect.visitor.dto.VisitorResponseDTO;
import com.smartsocietyconnect.visitor.enums.VisitorStatus;

/**
 * Service contract for visitor management operations.
 *
 * <p>Defines business logic for creating visitor requests, approval/rejection,
 * check-in, check-out, fetching visitor records, filtering visitor history,
 * and deleting visitor entries.
 *
 * <p>Implementations should enforce visitor workflow rules, such as:
 * <ul>
 *     <li>Only {@code REQUESTED} visitors can be approved or rejected.</li>
 *     <li>Only {@code APPROVED} visitors can be checked in.</li>
 *     <li>Only {@code CHECKED_IN} visitors can be checked out.</li>
 *     <li>Rejected or checked-out visitors should not be reprocessed.</li>
 * </ul>
 */
public interface VisitorService {

    // ==========================================================================
    // 1. Create Visitor
    // ==========================================================================

    /**
     * Creates a new visitor request.
     *
     * <p>The service implementation should:
     * <ul>
     *     <li>Validate that the resident exists.</li>
     *     <li>Resolve and set the resident entity.</li>
     *     <li>Resolve and set the user who created the request.</li>
     *     <li>Default the visitor status to {@link VisitorStatus#REQUESTED}.</li>
     * </ul>
     *
     * @param request visitor creation request DTO
     * @return created visitor response
     */
    VisitorResponseDTO createVisitor(VisitorRequestDTO request);

    /** Stores an optional gate-security photo for an existing visitor request. */
    VisitorResponseDTO updateVisitorPhoto(Integer visitorId, MultipartFile photo);

    Resource loadVisitorPhoto(Integer visitorId);

    // ==========================================================================
    // 2. Approval Workflow
    // ==========================================================================

    /**
     * Approves or rejects a visitor request.
     *
     * <p>If {@code request.approved()} is {@code true}, status should become
     * {@link VisitorStatus#APPROVED}. If it is {@code false}, status should become
     * {@link VisitorStatus#REJECTED}.
     *
     * @param visitorId visitor primary key
     * @param request approval/rejection request DTO
     * @return updated visitor response
     */
    VisitorResponseDTO approveVisitor(
            Integer visitorId,
            VisitorApprovalDTO request
    );

    // ==========================================================================
    // 3. Check-In Workflow
    // ==========================================================================

    /**
     * Checks in an approved visitor.
     *
     * <p>The implementation should set {@code checkInTime} using server time
     * and update status to {@link VisitorStatus#CHECKED_IN}.
     *
     * @param visitorId visitor primary key
     * @param request check-in request DTO
     * @return checked-in visitor response
     */
    VisitorResponseDTO checkInVisitor(
            Integer visitorId,
            VisitorCheckInDTO request
    );

    // ==========================================================================
    // 4. Check-Out Workflow
    // ==========================================================================

    /**
     * Checks out a checked-in visitor.
     *
     * <p>The implementation should set {@code checkOutTime} using server time
     * and update status to {@link VisitorStatus#CHECKED_OUT}.
     *
     * @param visitorId visitor primary key
     * @param request check-out request DTO
     * @return checked-out visitor response
     */
    VisitorResponseDTO checkOutVisitor(
            Integer visitorId,
            VisitorCheckOutDTO request
    );

    // ==========================================================================
    // 5. Read Operations
    // ==========================================================================

    /**
     * Fetches a visitor by ID.
     *
     * @param visitorId visitor primary key
     * @return matching visitor response
     */
    VisitorResponseDTO getVisitorById(Integer visitorId);

    /**
     * Fetches all visitor records.
     *
     * @return list of visitor responses
     */
    List<VisitorResponseDTO> getAllVisitors();

    /**
     * Fetches all visitors linked to a resident.
     *
     * @param residentId resident primary key
     * @return list of visitors for the resident
     */
    List<VisitorResponseDTO> getVisitorsByResident(Integer residentId);

    /**
     * Fetches visitors by workflow status.
     *
     * @param status visitor status
     * @return list of visitors matching the status
     */
    List<VisitorResponseDTO> getVisitorsByStatus(VisitorStatus status);

    /**
     * Fetches visitors by visit date.
     *
     * @param visitDate scheduled visit date
     * @return list of visitors scheduled on the given date
     */
    List<VisitorResponseDTO> getVisitorsByVisitDate(LocalDate visitDate);

    // ==========================================================================
    // 6. Delete
    // ==========================================================================

    /**
     * Deletes a visitor record.
     *
     * <p>Depending on business rules, implementations may prevent deletion of
     * checked-in visitors or preserve visitor history instead of hard deleting.
     *
     * @param visitorId visitor primary key
     */
    void deleteVisitor(Integer visitorId);
}
