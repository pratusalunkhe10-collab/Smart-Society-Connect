package com.smartsocietyconnect.complaint.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.complaint.dto.request.CreateComplaintRequest;
import com.smartsocietyconnect.complaint.dto.request.UpdateComplaintRequest;
import com.smartsocietyconnect.complaint.dto.request.UpdateComplaintStatusRequest;
import com.smartsocietyconnect.complaint.dto.response.ComplaintResponse;
import com.smartsocietyconnect.complaint.entity.Complaint;
import com.smartsocietyconnect.complaint.enums.ComplaintCategory;
import com.smartsocietyconnect.complaint.enums.ComplaintPriority;
import com.smartsocietyconnect.complaint.enums.ComplaintStatus;
import com.smartsocietyconnect.complaint.exception.ComplaintNotFoundException;
import com.smartsocietyconnect.complaint.exception.ComplaintOperationException;
import com.smartsocietyconnect.complaint.exception.DuplicateComplaintException;
import com.smartsocietyconnect.complaint.exception.InvalidComplaintStatusException;
import com.smartsocietyconnect.complaint.repository.ComplaintRepository;
import com.smartsocietyconnect.complaint.service.ComplaintService;
import com.smartsocietyconnect.resident.entity.Resident;
import com.smartsocietyconnect.resident.exception.ResidentException;
import com.smartsocietyconnect.resident.repository.ResidentRepository;
import com.smartsocietyconnect.resident.service.FileStorageService;
import com.smartsocietyconnect.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link ComplaintService}.
 *
 * <p>Handles all complaint-management business logic:
 * <ul>
 *     <li>Creating complaints</li>
 *     <li>Updating editable complaint details</li>
 *     <li>Deleting complaints</li>
 *     <li>Fetching complaints by ID, number, resident, status, priority, and category</li>
 *     <li>Managing complaint workflow/status transitions</li>
 * </ul>
 *
 * <p><b>Complaint workflow:</b>
 * <pre>
 * OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED
 * OPEN -> REJECTED
 * </pre>
 *
 * <p><b>Important implementation note:</b>
 * {@code complaint_number} is {@code NOT NULL} in the database, so the complaint
 * number must be generated before the first save. Do not save a complaint first
 * with {@code complaintNumber = null}; MySQL will reject it.
 *
 * <p><b>Transaction strategy:</b>
 * Class-level {@code @Transactional} covers write operations. Read methods
 * override it with {@code @Transactional(readOnly = true)} for better
 * Hibernate performance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ComplaintServiceImpl implements ComplaintService {

    private static final int MAX_COMPLAINT_NUMBER_ATTEMPTS = 5;

    private final ComplaintRepository complaintRepository;
    private final ResidentRepository residentRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    // ==========================================================================
    // 1. Create Complaint
    // ==========================================================================

    /**
     * Creates a new complaint.
     *
     * <p>Business flow:
     * <ol>
     *     <li>Validate that resident exists.</li>
     *     <li>Generate a unique complaint number.</li>
     *     <li>Build complaint entity.</li>
     *     <li>Default priority to {@code MEDIUM} if not provided.</li>
     *     <li>Default status to {@code OPEN}.</li>
     *     <li>Save complaint.</li>
     *     <li>Return response DTO.</li>
     * </ol>
     *
     * @param request complaint creation request
     * @return created complaint response
     */
    @Override
    public ComplaintResponse createComplaint(CreateComplaintRequest request) {
        log.info("Creating complaint for resident id: {}", request.getResidentId());

        Resident resident = getResidentOrThrow(request.getResidentId());
        String complaintNumber = generateUniqueComplaintNumber();

        Complaint complaint = Complaint.builder()
                .complaintNumber(complaintNumber)
                .resident(resident)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .priority(request.getPriority() != null
                        ? request.getPriority()
                        : ComplaintPriority.MEDIUM)
                .status(ComplaintStatus.OPEN)
                .attachment(request.getAttachment())
                .build();

        Complaint savedComplaint = complaintRepository.save(complaint);

        log.info("Complaint created successfully. complaintNumber={}", complaintNumber);

        return mapToResponse(savedComplaint);
    }

    // ==========================================================================
    // 2. Update Complaint Details
    // ==========================================================================

    /**
     * Updates editable complaint details.
     *
     * <p>Only {@code OPEN} complaints can be edited. Once work has started,
     * residents should not modify the complaint details directly.
     *
     * @param complaintId complaint primary key
     * @param request complaint update request
     * @return updated complaint response
     */
    @Override
    public ComplaintResponse updateComplaint(
            Integer complaintId,
            UpdateComplaintRequest request
    ) {
        log.info("Updating complaint id: {}", complaintId);

        Complaint complaint = getComplaintOrThrow(complaintId);

        if (!ComplaintStatus.OPEN.equals(complaint.getStatus())) {
            throw new ComplaintOperationException("Only OPEN complaints can be updated.");
        }

        if (request.getTitle() != null) {
            complaint.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            complaint.setDescription(request.getDescription());
        }

        if (request.getCategory() != null) {
            complaint.setCategory(request.getCategory());
        }

        if (request.getPriority() != null) {
            complaint.setPriority(request.getPriority());
        }

        if (request.getAttachment() != null) {
            complaint.setAttachment(request.getAttachment());
        }

        Complaint updatedComplaint = complaintRepository.save(complaint);

        log.info("Complaint updated successfully. complaintId={}", complaintId);

        return mapToResponse(updatedComplaint);
    }

    @Override
    public ComplaintResponse uploadAttachment(Integer complaintId, MultipartFile file) {
        Complaint complaint = getComplaintOrThrow(complaintId);
        String previousAttachment = complaint.getAttachment();
        complaint.setAttachment(fileStorageService.storeFile(file));
        ComplaintResponse response = mapToResponse(complaintRepository.save(complaint));
        if (previousAttachment != null && !previousAttachment.isBlank()) fileStorageService.deleteFile(previousAttachment);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadAttachment(Integer complaintId) {
        return fileStorageService.loadFile(getComplaintOrThrow(complaintId).getAttachment());
    }

    // ==========================================================================
    // 3. Delete Complaint
    // ==========================================================================

    /**
     * Deletes a complaint.
     *
     * <p>Only {@code OPEN} complaints can be deleted. Complaints already being
     * processed should be preserved for audit/history.
     *
     * @param complaintId complaint primary key
     */
    @Override
    public void deleteComplaint(Integer complaintId) {
        log.info("Deleting complaint id: {}", complaintId);

        Complaint complaint = getComplaintOrThrow(complaintId);

        if (!ComplaintStatus.OPEN.equals(complaint.getStatus())) {
            throw new ComplaintOperationException("Only OPEN complaints can be deleted.");
        }

        complaintRepository.delete(complaint);

        log.info("Complaint deleted successfully. complaintId={}", complaintId);
    }

    // ==========================================================================
    // 4. Read Operations
    // ==========================================================================

    /**
     * Fetches a complaint by ID.
     *
     * @param complaintId complaint primary key
     * @return complaint response
     */
    @Override
    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintById(Integer complaintId) {
        log.info("Fetching complaint by id: {}", complaintId);

        return mapToResponse(getComplaintOrThrow(complaintId));
    }

    /**
     * Fetches a complaint by public complaint number.
     *
     * @param complaintNumber complaint tracking number
     * @return complaint response
     */
    @Override
    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintByNumber(String complaintNumber) {
        log.info("Fetching complaint by number: {}", complaintNumber);

        Complaint complaint = complaintRepository.findByComplaintNumber(complaintNumber)
                .orElseThrow(() -> new ComplaintNotFoundException(
                        "Complaint not found with number: " + complaintNumber));

        return mapToResponse(complaint);
    }

    /**
     * Fetches all complaints ordered by latest first.
     *
     * @return list of complaint responses
     */
    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getAllComplaints() {
        log.info("Fetching all complaints");

        return complaintRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Fetches all complaints raised by a resident.
     *
     * @param residentId resident primary key
     * @return complaints raised by resident
     */
    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getComplaintsByResident(Integer residentId) {
        log.info("Fetching complaints for resident id: {}", residentId);

        if (!residentRepository.existsById(residentId)) {
            throw new ResidentException("Resident not found with id: " + residentId);
        }

        return complaintRepository.findByResidentResidentId(residentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Fetches complaints by status.
     *
     * @param status complaint status
     * @return complaints matching status
     */
    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getComplaintsByStatus(ComplaintStatus status) {
        log.info("Fetching complaints by status: {}", status);

        return complaintRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Fetches complaints by priority.
     *
     * @param priority complaint priority
     * @return complaints matching priority
     */
    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getComplaintsByPriority(ComplaintPriority priority) {
        log.info("Fetching complaints by priority: {}", priority);

        return complaintRepository.findByPriority(priority)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Fetches complaints by category.
     *
     * @param category complaint category
     * @return complaints matching category
     */
    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getComplaintsByCategory(ComplaintCategory category) {
        log.info("Fetching complaints by category: {}", category);

        return complaintRepository.findByCategory(category)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ==========================================================================
    // 5. Status Workflow
    // ==========================================================================

    /**
     * Updates complaint workflow status.
     *
     * <p>Valid transitions:
     * <pre>
     * OPEN        -> IN_PROGRESS
     * OPEN        -> REJECTED
     * IN_PROGRESS -> RESOLVED
     * RESOLVED    -> CLOSED
     * </pre>
     *
     * <p>When status becomes {@code RESOLVED}, {@code resolvedAt} is set using
     * server time.
     *
     * @param complaintId complaint primary key
     * @param request status update request
     * @return updated complaint response
     */
    @Override
    public ComplaintResponse updateComplaintStatus(
            Integer complaintId,
            UpdateComplaintStatusRequest request
    ) {
        log.info("Updating complaint status. complaintId={}", complaintId);

        Complaint complaint = getComplaintOrThrow(complaintId);

        ComplaintStatus currentStatus = complaint.getStatus();
        ComplaintStatus newStatus = request.getStatus();

        validateStatusTransition(currentStatus, newStatus);

        complaint.setStatus(newStatus);

        if (request.getAssignedTo() != null) {
            complaint.setAssignedTo(request.getAssignedTo());
        }

        if (request.getResolutionRemarks() != null) {
            complaint.setResolutionRemarks(request.getResolutionRemarks());
        }

        if (ComplaintStatus.RESOLVED.equals(newStatus)) {
            complaint.setResolvedAt(LocalDateTime.now());
        }

        Complaint updatedComplaint = complaintRepository.save(complaint);

        if (ComplaintStatus.RESOLVED.equals(newStatus) && !ComplaintStatus.RESOLVED.equals(currentStatus)) {
            notificationService.notifyUser(complaint.getResident().getUser(), "COMPLAINT",
                    "Complaint resolved: " + complaint.getTitle(),
                    "Your complaint " + complaint.getComplaintNumber() + " has been resolved."
                            + (complaint.getResolutionRemarks() == null || complaint.getResolutionRemarks().isBlank()
                                    ? "" : " Resolution: " + complaint.getResolutionRemarks()),
                    "/app/complaints");
        }

        log.info(
                "Complaint status updated successfully. complaintId={}, status={}",
                complaintId,
                newStatus
        );

        return mapToResponse(updatedComplaint);
    }

    // ==========================================================================
    // Private Helpers
    // ==========================================================================

    /**
     * Finds resident by ID or throws a resident-module exception.
     *
     * @param residentId resident primary key
     * @return resident entity
     */
    private Resident getResidentOrThrow(Integer residentId) {
        return residentRepository.findById(residentId)
                .orElseThrow(() -> new ResidentException(
                        "Resident not found with id: " + residentId));
    }

    /**
     * Finds complaint by ID or throws a complaint-module exception.
     *
     * @param complaintId complaint primary key
     * @return complaint entity
     */
    private Complaint getComplaintOrThrow(Integer complaintId) {
        return complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ComplaintNotFoundException(
                        "Complaint not found with id: " + complaintId));
    }

    /**
     * Generates a unique complaint number.
     *
     * <p>Format is {@code CMP-XXXXXXXXXXXXXXX}. Length is exactly 19 characters,
     * under the database limit of {@code VARCHAR(20)}.
     *
     * <p>UUID is used because {@code complaint_number} is NOT NULL and UNIQUE,
     * so the number must exist before the first insert.
     *
     * @return unique complaint number
     */
    private String generateUniqueComplaintNumber() {
        for (int attempt = 1; attempt <= MAX_COMPLAINT_NUMBER_ATTEMPTS; attempt++) {
            String complaintNumber = "CMP-" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 15)
                    .toUpperCase();

            if (!complaintRepository.existsByComplaintNumber(complaintNumber)) {
                return complaintNumber;
            }
        }

        throw new DuplicateComplaintException(
                "Unable to generate unique complaint number. Please try again.");
    }

    /**
     * Validates complaint status transition.
     *
     * @param currentStatus current complaint status
     * @param newStatus requested new status
     */
    private void validateStatusTransition(
            ComplaintStatus currentStatus,
            ComplaintStatus newStatus
    ) {
        if (currentStatus == null || newStatus == null) {
            throw new InvalidComplaintStatusException("Complaint status cannot be null.");
        }

        if (currentStatus.equals(newStatus)) {
            throw new InvalidComplaintStatusException(
                    "Complaint is already in status: " + currentStatus);
        }

        switch (currentStatus) {
            case OPEN -> {
                if (!ComplaintStatus.IN_PROGRESS.equals(newStatus)
                        && !ComplaintStatus.REJECTED.equals(newStatus)) {
                    throw new InvalidComplaintStatusException(
                            "OPEN complaint can move only to IN_PROGRESS or REJECTED.");
                }
            }

            case IN_PROGRESS -> {
                if (!ComplaintStatus.RESOLVED.equals(newStatus)) {
                    throw new InvalidComplaintStatusException(
                            "IN_PROGRESS complaint can move only to RESOLVED.");
                }
            }

            case RESOLVED -> {
                if (!ComplaintStatus.CLOSED.equals(newStatus)) {
                    throw new InvalidComplaintStatusException(
                            "RESOLVED complaint can move only to CLOSED.");
                }
            }

            case CLOSED -> throw new InvalidComplaintStatusException(
                    "CLOSED complaint cannot be modified.");

            case REJECTED -> throw new InvalidComplaintStatusException(
                    "REJECTED complaint cannot be modified.");
        }
    }

    /**
     * Maps {@link Complaint} entity to {@link ComplaintResponse}.
     *
     * @param complaint complaint entity
     * @return complaint response DTO
     */
    private ComplaintResponse mapToResponse(Complaint complaint) {
        Resident resident = complaint.getResident();

        return ComplaintResponse.builder()
                .complaintId(complaint.getComplaintId())
                .complaintNumber(complaint.getComplaintNumber())
                .residentId(resident != null ? resident.getResidentId() : null)
                .residentName(resolveResidentName(resident))
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(complaint.getCategory())
                .priority(complaint.getPriority())
                .status(complaint.getStatus())
                .attachment(complaint.getAttachment())
                .assignedTo(complaint.getAssignedTo())
                .resolutionRemarks(complaint.getResolutionRemarks())
                .resolvedAt(complaint.getResolvedAt())
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .build();
    }

    /**
     * Resolves resident full name from linked user.
     *
     * @param resident resident entity
     * @return full name, or null if unavailable
     */
    private String resolveResidentName(Resident resident) {
        if (resident == null || resident.getUser() == null) {
            return null;
        }

        User user = resident.getUser();

        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";

        String fullName = (firstName + " " + lastName).trim();

        return fullName.isEmpty() ? null : fullName;
    }
}
