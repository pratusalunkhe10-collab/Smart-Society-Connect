package com.smartsocietyconnect.auth.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserResponseDto(
        Integer id,
        String name,
        String email,
        String mobile,
        boolean verified,
        boolean active,
        String approvalStatus,
        List<String> roles,
        LocalDateTime registeredAt
) {}
