package com.smartsocietyconnect.visitor.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.auth.repository.UserRepository;
import com.smartsocietyconnect.resident.entity.Resident;
import com.smartsocietyconnect.resident.exception.ResidentException;
import com.smartsocietyconnect.resident.repository.ResidentRepository;
import com.smartsocietyconnect.resident.service.FileStorageService;
import com.smartsocietyconnect.visitor.dto.VisitorApprovalDTO;
import com.smartsocietyconnect.visitor.dto.VisitorCheckInDTO;
import com.smartsocietyconnect.visitor.dto.VisitorCheckOutDTO;
import com.smartsocietyconnect.visitor.dto.VisitorRequestDTO;
import com.smartsocietyconnect.visitor.dto.VisitorResponseDTO;
import com.smartsocietyconnect.visitor.entity.Visitor;
import com.smartsocietyconnect.visitor.enums.VisitorStatus;
import com.smartsocietyconnect.visitor.exception.DuplicateVisitorException;
import com.smartsocietyconnect.visitor.exception.InvalidVisitorStatusException;
import com.smartsocietyconnect.visitor.exception.VisitorNotFoundException;
import com.smartsocietyconnect.visitor.mapper.VisitorMapper;
import com.smartsocietyconnect.visitor.repository.VisitorRepository;
import com.smartsocietyconnect.visitor.service.VisitorService;
import com.smartsocietyconnect.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link VisitorService}.
 *
 * <p>Handles visitor request creation, approval/rejection, check-in,
 * check-out, visitor lookup, filtering, and deletion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VisitorServiceImpl implements VisitorService {

    private static final Integer TEMP_USER_ID = 1;

    private final VisitorRepository visitorRepository;
    private final ResidentRepository residentRepository;
    private final UserRepository userRepository;
    private final VisitorMapper visitorMapper;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    // ==========================================================================
    // 1. Create Visitor
    // ==========================================================================

    @Override
    public VisitorResponseDTO createVisitor(VisitorRequestDTO request) {
        log.info("Creating visitor request for resident id: {}", request.residentId());

        if (visitorRepository.existsByIdProofNumber(request.idProofNumber())) {
            throw new DuplicateVisitorException(request.idProofNumber());
        }

        Resident resident = findResidentById(request.residentId());
        User createdBy = getTemporaryUser();

        Visitor visitor = visitorMapper.toEntity(request);
        visitor.setResident(resident);
        visitor.setCreatedBy(createdBy);
        visitor.setStatus(VisitorStatus.REQUESTED);

        Visitor savedVisitor = visitorRepository.save(visitor);

        notificationService.notifyUser(resident.getUser(), "VISITOR", "Visitor approval requested",
                savedVisitor.getVisitorName() + " has requested to visit you. Please review the visitor request.", "/app/visitors");

        log.info("Visitor request created with id: {}", savedVisitor.getVisitorId());

        return visitorMapper.toResponseDTO(savedVisitor);
    }

    @Override
    public VisitorResponseDTO updateVisitorPhoto(Integer visitorId, MultipartFile photo) {
        Visitor visitor = findVisitorById(visitorId);
        String previousPath = visitor.getPhotoUrl();
        visitor.setPhotoUrl(fileStorageService.storeFile(photo));
        VisitorResponseDTO response = visitorMapper.toResponseDTO(visitorRepository.save(visitor));
        if (previousPath != null && !previousPath.isBlank()) {
            fileStorageService.deleteFile(previousPath);
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadVisitorPhoto(Integer visitorId) {
        return fileStorageService.loadFile(findVisitorById(visitorId).getPhotoUrl());
    }

    // ==========================================================================
    // 2. Approve / Reject Visitor
    // ==========================================================================

    @Override
    public VisitorResponseDTO approveVisitor(
            Integer visitorId,
            VisitorApprovalDTO request
    ) {
        log.info("Processing approval decision for visitor id: {}", visitorId);

        Visitor visitor = findVisitorById(visitorId);

        validateStatus(visitor, VisitorStatus.REQUESTED);

        User approvedBy = getTemporaryUser();

        visitor.setApprovedBy(approvedBy);
        visitor.setRemarks(request.remarks());
        visitor.setStatus(Boolean.TRUE.equals(request.approved())
                ? VisitorStatus.APPROVED
                : VisitorStatus.REJECTED);

        Visitor updatedVisitor = visitorRepository.save(visitor);

        notificationService.notifyUser(visitor.getResident().getUser(), "VISITOR",
                Boolean.TRUE.equals(request.approved()) ? "Visitor request approved" : "Visitor request declined",
                visitor.getVisitorName() + " has been " + (Boolean.TRUE.equals(request.approved()) ? "approved" : "declined") + ".", "/app/visitors");

        return visitorMapper.toResponseDTO(updatedVisitor);
    }

    // ==========================================================================
    // 3. Check-In Visitor
    // ==========================================================================

    @Override
    public VisitorResponseDTO checkInVisitor(
            Integer visitorId,
            VisitorCheckInDTO request
    ) {
        log.info("Checking in visitor id: {}", visitorId);

        Visitor visitor = findVisitorById(visitorId);

        validateStatus(visitor, VisitorStatus.APPROVED);

        if (request.vehicleNumber() != null) {
            visitor.setVehicleNumber(request.vehicleNumber());
        }

        if (request.remarks() != null) {
            visitor.setRemarks(request.remarks());
        }

        visitor.setCheckInTime(LocalDateTime.now());
        visitor.setStatus(VisitorStatus.CHECKED_IN);

        Visitor updatedVisitor = visitorRepository.save(visitor);

        return visitorMapper.toResponseDTO(updatedVisitor);
    }

    // ==========================================================================
    // 4. Check-Out Visitor
    // ==========================================================================

    @Override
    public VisitorResponseDTO checkOutVisitor(
            Integer visitorId,
            VisitorCheckOutDTO request
    ) {
        log.info("Checking out visitor id: {}", visitorId);

        Visitor visitor = findVisitorById(visitorId);

        validateStatus(visitor, VisitorStatus.CHECKED_IN);

        if (request.remarks() != null) {
            visitor.setRemarks(request.remarks());
        }

        visitor.setCheckOutTime(LocalDateTime.now());
        visitor.setStatus(VisitorStatus.CHECKED_OUT);

        Visitor updatedVisitor = visitorRepository.save(visitor);

        return visitorMapper.toResponseDTO(updatedVisitor);
    }

    // ==========================================================================
    // 5. Read Operations
    // ==========================================================================

    @Override
    @Transactional(readOnly = true)
    public VisitorResponseDTO getVisitorById(Integer visitorId) {
        return visitorMapper.toResponseDTO(findVisitorById(visitorId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitorResponseDTO> getAllVisitors() {
        return visitorRepository.findAll()
                .stream()
                .map(visitorMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitorResponseDTO> getVisitorsByResident(Integer residentId) {
        if (!residentRepository.existsById(residentId)) {
            throw new ResidentException("Resident not found with id: " + residentId);
        }

        return visitorRepository.findByResidentResidentId(residentId)
                .stream()
                .map(visitorMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitorResponseDTO> getVisitorsByStatus(VisitorStatus status) {
        return visitorRepository.findByStatus(status)
                .stream()
                .map(visitorMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitorResponseDTO> getVisitorsByVisitDate(LocalDate visitDate) {
        return visitorRepository.findByVisitDate(visitDate)
                .stream()
                .map(visitorMapper::toResponseDTO)
                .toList();
    }

    // ==========================================================================
    // 6. Delete Visitor
    // ==========================================================================

    @Override
    public void deleteVisitor(Integer visitorId) {
        log.info("Deleting visitor id: {}", visitorId);

        Visitor visitor = findVisitorById(visitorId);

        if (VisitorStatus.CHECKED_IN.equals(visitor.getStatus())) {
            throw new InvalidVisitorStatusException(
                    visitor.getStatus(),
                    VisitorStatus.CHECKED_OUT
            );
        }

        visitorRepository.delete(visitor);

        log.info("Visitor deleted. visitorId={}", visitorId);
    }

    // ==========================================================================
    // Private Helpers
    // ==========================================================================

    private Visitor findVisitorById(Integer visitorId) {
        return visitorRepository.findById(visitorId)
                .orElseThrow(() -> new VisitorNotFoundException(visitorId));
    }

    private Resident findResidentById(Integer residentId) {
        return residentRepository.findById(residentId)
                .orElseThrow(() -> new ResidentException(
                        "Resident not found with id: " + residentId));
    }

    private User getTemporaryUser() {
        return userRepository.findById(TEMP_USER_ID)
                .orElseThrow(() -> new ResidentException(
                        "User not found with id: " + TEMP_USER_ID));
    }

    private void validateStatus(
            Visitor visitor,
            VisitorStatus expectedStatus
    ) {
        if (!expectedStatus.equals(visitor.getStatus())) {
            throw new InvalidVisitorStatusException(
                    visitor.getStatus(),
                    expectedStatus
            );
        }
    }
}
