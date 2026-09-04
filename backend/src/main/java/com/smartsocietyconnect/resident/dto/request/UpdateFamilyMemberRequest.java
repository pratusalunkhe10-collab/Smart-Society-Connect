package com.smartsocietyconnect.resident.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO used to partially update an existing family member.
 *
 * <p>All fields are optional because this DTO is intended for update/PATCH-like
 * operations. The service layer should apply only non-null values and leave
 * existing values unchanged when a field is not provided.
 */
@Getter
@Setter
public class UpdateFamilyMemberRequest {

    /**
     * Updated name of the family member.
     *
     * <p>Nullable for partial update. If provided, it must not exceed the
     * expected database column length.
     */
    @Size(max = 100, message = "Member name must not exceed 100 characters")
    private String memberName;

    /**
     * Updated age of the family member.
     *
     * <p>Nullable for partial update. If provided, it must be within a realistic
     * human age range.
     */
    @Min(value = 0, message = "Age cannot be negative")
    @Max(value = 120, message = "Age cannot exceed 120")
    private Byte  age;

    /**
     * Updated mobile number of the family member.
     *
     * <p>Nullable for partial update. If provided, it must contain exactly
     * 10 digits.
     */
    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "Mobile number must be exactly 10 digits"
    )
    private String mobile;
}