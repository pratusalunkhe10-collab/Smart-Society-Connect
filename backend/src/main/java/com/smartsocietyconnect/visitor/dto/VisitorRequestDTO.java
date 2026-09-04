package com.smartsocietyconnect.visitor.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.smartsocietyconnect.visitor.enums.Gender;
import com.smartsocietyconnect.visitor.enums.IdProofType;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Request DTO used to create a new visitor entry.
 *
 * <p>This DTO carries all information required when a resident/admin/security
 * user creates a visitor request.
 *
 * <p>Maps mostly to the {@code visitors} table, except fields such as
 * {@code status}, {@code checkInTime}, {@code checkOutTime}, {@code approvedBy},
 * {@code createdAt}, and {@code updatedAt}, which are controlled by the service
 * layer and database.
 *
 * <p>Visitor status should default to {@code REQUESTED} during creation.
 */
@Builder
public record VisitorRequestDTO(

        /**
         * Resident being visited.
         *
         * <p>References {@code residents.resident_id}.
         */
        @NotNull(message = "Resident ID is required")
        Integer residentId,

        /**
         * Full name of the visitor.
         */
        @NotBlank(message = "Visitor name is required")
        @Size(max = 100, message = "Visitor name must not exceed 100 characters")
        String visitorName,

        /**
         * Indian 10-digit mobile number.
         */
        @NotBlank(message = "Mobile number is required")
        @Pattern(
                regexp = "^[6-9]\\d{9}$",
                message = "Mobile number must be a valid 10-digit Indian mobile number"
        )
        String mobile,

        /**
         * Visitor gender.
         */
        @NotNull(message = "Gender is required")
        Gender gender,

        /**
         * Visitor age.
         *
         * <p>Maps to {@code visitors.age}, which is {@code TINYINT UNSIGNED}.
         */
        @NotNull(message = "Age is required")
        @Min(value = 1, message = "Age must be at least 1")
        @Max(value = 120, message = "Age must not exceed 120")
        Short age,

        /**
         * Visitor address.
         */
        @NotBlank(message = "Address is required")
        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        /**
         * Type of ID proof submitted by visitor.
         */
        @NotNull(message = "ID proof type is required")
        IdProofType idProofType,

        /**
         * ID proof number.
         */
        @NotBlank(message = "ID proof number is required")
        @Size(max = 30, message = "ID proof number must not exceed 30 characters")
        String idProofNumber,

        /**
         * Optional visitor photo URL/path.
         */
        @Size(max = 255, message = "Photo URL must not exceed 255 characters")
        String photoUrl,

        /**
         * Optional visitor vehicle number.
         */
        @Size(max = 20, message = "Vehicle number must not exceed 20 characters")
        String vehicleNumber,

        /**
         * Purpose of visit.
         */
        @NotBlank(message = "Purpose is required")
        @Size(max = 255, message = "Purpose must not exceed 255 characters")
        String purpose,

        /**
         * Planned visit date.
         */
        @NotNull(message = "Visit date is required")
        @FutureOrPresent(message = "Visit date cannot be in the past")
        LocalDate visitDate,

        /**
         * Optional expected arrival time.
         */
        LocalTime expectedTime,

        /**
         * Optional remarks added during visitor request creation.
         */
        @Size(max = 255, message = "Remarks must not exceed 255 characters")
        String remarks

) {}