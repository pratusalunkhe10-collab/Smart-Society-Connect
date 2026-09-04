package com.smartsocietyconnect.resident.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartsocietyconnect.resident.dto.request.CreateFlatRequest;
import com.smartsocietyconnect.resident.dto.request.UpdateFlatRequest;
import com.smartsocietyconnect.resident.dto.response.FlatResponse;
import com.smartsocietyconnect.resident.entity.Flat;
import com.smartsocietyconnect.resident.entity.FlatStatus;
import com.smartsocietyconnect.resident.entity.FlatType;
import com.smartsocietyconnect.resident.exception.ResidentException;
import com.smartsocietyconnect.resident.repository.FlatRepository;
import com.smartsocietyconnect.resident.service.FlatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link FlatService}.
 *
 * <p>Handles flat creation, reading, filtering, updating, status changes,
 * deletion, and entity-to-response mapping.
 *
 * <p>Class-level {@code @Transactional} covers write operations. Read-only
 * methods override it with {@code @Transactional(readOnly = true)} for better
 * Hibernate performance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FlatServiceImpl implements FlatService {

    private final FlatRepository flatRepository;

    // ==========================================================================
    // CREATE
    // ==========================================================================

    /**
     * Creates a new flat after validating duplicate flat number.
     *
     * @param request flat creation request
     * @return created flat response
     */
    @Override
    public FlatResponse createFlat(CreateFlatRequest request) {
        log.info("Creating flat with number: {}", request.getFlatNumber());

        if (flatRepository.existsByFlatNumber(request.getFlatNumber())) {
            throw new ResidentException("Flat number already exists: " + request.getFlatNumber());
        }

        Flat flat = Flat.builder()
                .flatNumber(request.getFlatNumber())
                .wing(request.getWing())
                .floorNumber(request.getFloorNumber().shortValue())
                .flatType(request.getFlatType())
                .areaSqft(request.getAreaSqft())
                .build();

        return mapToResponse(flatRepository.save(flat));
    }

    // ==========================================================================
    // READ
    // ==========================================================================

    @Override
    @Transactional(readOnly = true)
    public FlatResponse getFlatById(Integer flatId) {
        return mapToResponse(findFlatById(flatId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlatResponse> getAllFlats() {
        return flatRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlatResponse> getFlatsByWing(String wing) {
        return flatRepository.findByWing(wing)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlatResponse> getFlatsByStatus(FlatStatus status) {
        return flatRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlatResponse> getFlatsByType(FlatType flatType) {
        return flatRepository.findByFlatType(flatType)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ==========================================================================
    // UPDATE
    // ==========================================================================

    @Override
    public FlatResponse updateFlat(Integer flatId, UpdateFlatRequest request) {
        log.info("Updating flat id: {}", flatId);

        Flat flat = findFlatById(flatId);

        if (request.getFlatNumber() != null
                && !request.getFlatNumber().equals(flat.getFlatNumber())
                && flatRepository.existsByFlatNumber(request.getFlatNumber())) {
            throw new ResidentException("Flat number already exists: " + request.getFlatNumber());
        }

        if (request.getFlatNumber() != null) {
            flat.setFlatNumber(request.getFlatNumber());
        }
        if (request.getWing() != null) {
            flat.setWing(request.getWing());
        }
        if (request.getFloorNumber() != null) {
            flat.setFloorNumber(request.getFloorNumber().shortValue());
        }
        if (request.getFlatType() != null) {
            flat.setFlatType(request.getFlatType());
        }
        if (request.getAreaSqft() != null) {
            flat.setAreaSqft(request.getAreaSqft());
        }
        if (request.getStatus() != null) {
            flat.setStatus(request.getStatus());
        }

        return mapToResponse(flatRepository.save(flat));
    }

    @Override
    public FlatResponse updateFlatStatus(Integer flatId, FlatStatus status) {
        Flat flat = findFlatById(flatId);
        flat.setStatus(status);

        return mapToResponse(flatRepository.save(flat));
    }

    // ==========================================================================
    // DELETE
    // ==========================================================================

    @Override
    public void deleteFlat(Integer flatId) {
        Flat flat = findFlatById(flatId);

        if (FlatStatus.OCCUPIED.equals(flat.getStatus())) {
            throw new ResidentException(
                    "Cannot delete flat id: " + flatId + " because flat is currently OCCUPIED"
            );
        }

        flatRepository.delete(flat);
    }

    // ==========================================================================
    // Private Helpers
    // ==========================================================================

    private Flat findFlatById(Integer flatId) {
        return flatRepository.findById(flatId)
                .orElseThrow(() -> new ResidentException("Flat not found with id: " + flatId));
    }

    private FlatResponse mapToResponse(Flat flat) {
        return FlatResponse.builder()
                .flatId(flat.getFlatId())
                .flatNumber(flat.getFlatNumber())
                .wing(flat.getWing())
                .floorNumber(flat.getFloorNumber())
                .flatType(flat.getFlatType())
                .areaSqft(flat.getAreaSqft())
                .status(flat.getStatus())
                .createdAt(flat.getCreatedAt())
                .build();
    }
}