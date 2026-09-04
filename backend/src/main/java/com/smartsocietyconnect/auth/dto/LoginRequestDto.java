package com.smartsocietyconnect.auth.dto;

//import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO used for login requests.
 *
 * <p>Accepts either email or mobile number as username identifier.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDto {

    /**
     * User identifier (email or mobile number).
     */
    @NotBlank(message = "Email or mobile is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    /**
     * User password.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters")
    //@JsonIgnore // Ensures password is never serialized in responses
    private String password;
}