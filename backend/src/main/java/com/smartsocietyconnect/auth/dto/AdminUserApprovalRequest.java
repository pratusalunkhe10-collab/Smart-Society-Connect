package com.smartsocietyconnect.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminUserApprovalRequest(@NotBlank String role) {}
