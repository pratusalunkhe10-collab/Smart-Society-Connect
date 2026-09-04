package com.smartsocietyconnect.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO returned after successful authentication.
 *
 * <p>Contains JWT token, metadata, and user information.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {

    /**
     * JWT access token.
     */
    @JsonProperty("access_token")
    private String token;

    /**
     * Token type.
     */
    @JsonProperty("token_type")
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Token expiration time in seconds.
     *
     * Example:
     * 3600 (1 hour)
     */
    @JsonProperty("expires_in")
    private Long expiresIn;

    /**
     * User ID.
     */
    @JsonProperty("user_id")
    private Integer userId;

    /**
     * User full name.
     */
    @JsonProperty("full_name")
    private String fullName;

    /**
     * User email.
     */
    private String email;

    /**
     * User role name.
     */
    @JsonProperty("role")
    private String roleName;

    /** Public endpoint for the user's saved profile image, when available. */
    @JsonProperty("profile_image_url")
    private String profileImageUrl;
}
