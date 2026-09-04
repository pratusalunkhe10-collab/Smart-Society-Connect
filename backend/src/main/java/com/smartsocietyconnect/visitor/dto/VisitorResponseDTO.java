package com.smartsocietyconnect.visitor.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.smartsocietyconnect.visitor.enums.Gender;
import com.smartsocietyconnect.visitor.enums.IdProofType;
import com.smartsocietyconnect.visitor.enums.VisitorStatus;

import lombok.Builder;

/**
 * Response DTO returned after visitor-related operations.
 *
 * <p>Represents a flattened, read-only view of visitor data for API clients.
 *
 * <p>This DTO may combine data from:
 * <ul>
 *     <li>{@code visitors}</li>
 *     <li>{@code residents}</li>
 *     <li>{@code users}</li>
 * </ul>
 */
@Builder
public record VisitorResponseDTO(

        Integer visitorId,

        Integer residentId,

        String residentName,

        String visitorName,

        String mobile,

        Gender gender,

        Short age,

        String address,

        IdProofType idProofType,

        String idProofNumber,

        String photoUrl,

        String vehicleNumber,

        String purpose,

        LocalDate visitDate,

        LocalTime expectedTime,

        LocalDateTime checkInTime,

        LocalDateTime checkOutTime,

        VisitorStatus status,

        Integer approvedBy,

        Integer createdBy,

        String remarks,

        LocalDateTime createdAt,

        LocalDateTime updatedAt

) {}