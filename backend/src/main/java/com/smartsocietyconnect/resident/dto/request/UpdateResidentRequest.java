package com.smartsocietyconnect.resident.dto.request;

import java.time.LocalDate;

import com.smartsocietyconnect.resident.entity.ResidentType;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for partially updating a resident profile.
 *
 * All fields are optional. Only non-null fields should be applied
 * by the service layer.
 */
@Getter
@Setter
public class UpdateResidentRequest {

    private ResidentType residentType;

    @Size(max = 100, message = "Occupation must not exceed 100 characters")
    private String occupation;

    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "Emergency contact must be exactly 10 digits"
    )
    private String emergencyContact;

    @PastOrPresent(message = "Move-out date cannot be in the future")
    private LocalDate moveOutDate;

    private Boolean isPrimaryMember;
}