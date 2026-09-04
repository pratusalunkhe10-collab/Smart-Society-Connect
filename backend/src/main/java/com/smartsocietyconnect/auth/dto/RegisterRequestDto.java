package com.smartsocietyconnect.auth.dto;

//import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO used for user registration requests.
 *
 * <p>Contains user input required during registration.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequestDto {

    /**
     * User first name.
     */
    @NotBlank(message = "First name is required")
    @Size(max = 30, message = "First name cannot exceed 30 characters")
    private String firstName;

    /**
     * User last name.
     */
    @NotBlank(message = "Last name is required")
    @Size(max = 30, message = "Last name cannot exceed 30 characters")
    private String lastName;

    /**
     * User email address.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 50, message = "Email cannot exceed 50 characters")
    private String email;

    /**
     * User mobile number.
     * Supports formats: 9876543210, +919876543210, 919876543210
     */
    @NotBlank(message = "Mobile number is required")
    @Pattern(
        regexp = "^(?:\\+?91|0)?[6-9]\\d{9}$",
        message = "Invalid mobile number format"
    )
    private String mobile;

    /**
     * User password.
     * Requirements: 8-20 characters, at least one uppercase, one lowercase, one digit, and one special character.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
        message = "Password must contain at least one uppercase, one lowercase, one digit, and one special character"
    )
    //@JsonIgnore // Prevents password from being serialized in responses
    private String password;
}