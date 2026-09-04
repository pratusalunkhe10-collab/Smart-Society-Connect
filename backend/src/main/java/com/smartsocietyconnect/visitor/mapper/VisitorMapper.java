package com.smartsocietyconnect.visitor.mapper;

import org.springframework.stereotype.Component;

import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.resident.entity.Resident;
import com.smartsocietyconnect.visitor.dto.VisitorRequestDTO;
import com.smartsocietyconnect.visitor.dto.VisitorResponseDTO;
import com.smartsocietyconnect.visitor.entity.Visitor;

/**
 * Mapper component for converting between Visitor DTOs and Visitor entity.
 *
 * <p>This mapper handles only simple field-to-field conversion.
 *
 * <p><b>Important design rule:</b>
 * Relationship fields such as {@code resident}, {@code createdBy}, and
 * {@code approvedBy} are not set inside {@link #toEntity(VisitorRequestDTO)}.
 * These values must be resolved in the service layer using repositories because
 * they come from trusted server-side/database state, not directly from the client.
 *
 * <p>Example:
 * <pre>{@code
 * Resident resident = residentRepository.findById(dto.residentId())
 *         .orElseThrow(...);
 *
 * Visitor visitor = visitorMapper.toEntity(dto);
 * visitor.setResident(resident);
 * visitor.setCreatedBy(currentUser);
 * }</pre>
 */
@Component
public class VisitorMapper {

    // ==========================================================================
    // Request DTO To Entity
    // ==========================================================================

    /**
     * Converts {@link VisitorRequestDTO} to {@link Visitor}.
     *
     * <p>Only maps client-supplied visitor details. The service layer must set:
     * <ul>
     *     <li>{@code resident}</li>
     *     <li>{@code createdBy}</li>
     *     <li>{@code approvedBy}</li>
     *     <li>{@code status}, if overriding default workflow</li>
     * </ul>
     *
     * @param dto visitor creation request DTO
     * @return visitor entity with basic fields populated, or {@code null}
     */
    public Visitor toEntity(VisitorRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        return Visitor.builder()
                .visitorName(dto.visitorName())
                .mobile(dto.mobile())
                .gender(dto.gender())
                .age(dto.age())
                .address(dto.address())
                .idProofType(dto.idProofType())
                .idProofNumber(dto.idProofNumber())
                .photoUrl(dto.photoUrl())
                .vehicleNumber(dto.vehicleNumber())
                .purpose(dto.purpose())
                .visitDate(dto.visitDate())
                .expectedTime(dto.expectedTime())
                .remarks(dto.remarks())
                .build();
    }

    // ==========================================================================
    // Entity To Response DTO
    // ==========================================================================

    /**
     * Converts {@link Visitor} entity to {@link VisitorResponseDTO}.
     *
     * <p>Creates a flattened response object suitable for API clients. It includes
     * visitor details, resident information, workflow status, and audit fields.
     *
     * @param visitor visitor entity
     * @return response DTO, or {@code null}
     */
    public VisitorResponseDTO toResponseDTO(Visitor visitor) {
        if (visitor == null) {
            return null;
        }

        Resident resident = visitor.getResident();

        return VisitorResponseDTO.builder()
                .visitorId(visitor.getVisitorId())
                .residentId(resident != null ? resident.getResidentId() : null)
                .residentName(resolveResidentName(resident))
                .visitorName(visitor.getVisitorName())
                .mobile(visitor.getMobile())
                .gender(visitor.getGender())
                .age(visitor.getAge())
                .address(visitor.getAddress())
                .idProofType(visitor.getIdProofType())
                .idProofNumber(visitor.getIdProofNumber())
                .photoUrl(visitor.getPhotoUrl())
                .vehicleNumber(visitor.getVehicleNumber())
                .purpose(visitor.getPurpose())
                .visitDate(visitor.getVisitDate())
                .expectedTime(visitor.getExpectedTime())
                .checkInTime(visitor.getCheckInTime())
                .checkOutTime(visitor.getCheckOutTime())
                .status(visitor.getStatus())
                .approvedBy(visitor.getApprovedBy() != null
                        ? visitor.getApprovedBy().getUserId()
                        : null)
                .createdBy(visitor.getCreatedBy() != null
                        ? visitor.getCreatedBy().getUserId()
                        : null)
                .remarks(visitor.getRemarks())
                .createdAt(visitor.getCreatedAt())
                .updatedAt(visitor.getUpdatedAt())
                .build();
    }

    // ==========================================================================
    // Private Helpers
    // ==========================================================================

    /**
     * Resolves resident full name from linked user.
     *
     * <p>Resident name is stored in the linked {@code users} table, not directly
     * in the resident table.
     *
     * @param resident resident entity
     * @return full name, or {@code null} if unavailable
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