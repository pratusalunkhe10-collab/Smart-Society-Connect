package com.smartsocietyconnect.auth.dto;

import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;

@Builder
public record ProfileResponseDto(
        Integer id,
        String name,
        String email,
        String mobile,
        String flat,
        @JsonProperty("role") String roleName,
        String profileImageUrl
) {}
