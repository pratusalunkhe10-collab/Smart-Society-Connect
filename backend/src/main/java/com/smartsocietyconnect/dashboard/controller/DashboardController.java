package com.smartsocietyconnect.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.auth.repository.UserRepository;
import com.smartsocietyconnect.common.payload.ApiResponse;
import com.smartsocietyconnect.dashboard.dto.AccountantDashboardResponse;
import com.smartsocietyconnect.dashboard.dto.AdminDashboardResponse;
import com.smartsocietyconnect.dashboard.dto.ResidentDashboardResponse;
import com.smartsocietyconnect.dashboard.dto.SecurityDashboardResponse;
import com.smartsocietyconnect.dashboard.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for Dashboard Module.
 *
 * <p>Provides dashboard APIs for different user roles:
 * <ul>
 *     <li>Admin dashboard</li>
 *     <li>Resident dashboard</li>
 *     <li>Security dashboard</li>
 *     <li>Accountant dashboard</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(
        name = "Dashboard Management",
        description = "APIs for role-based dashboard statistics"
)
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    // ==========================================================================
    // 1. Admin Dashboard - GET /api/dashboard/admin
    // ==========================================================================

    @GetMapping("/admin")
    @Operation(
            summary = "Get Admin Dashboard",
            description = "Returns society-wide dashboard statistics for admin users."
    )
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getAdminDashboard() {

        AdminDashboardResponse response =
                dashboardService.getAdminDashboard();

        return ResponseEntity.ok(
                success("Admin dashboard fetched successfully", response)
        );
    }

    // ==========================================================================
    // 2. Resident Dashboard - GET /api/dashboard/resident
    // ==========================================================================

    @GetMapping("/resident")
    @Operation(
            summary = "Get Resident Dashboard",
            description = "Returns dashboard statistics for the logged-in resident."
    )
    public ResponseEntity<ApiResponse<ResidentDashboardResponse>> getResidentDashboard(
            Authentication authentication
    ) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Authenticated user not found: " + email)
                );

        ResidentDashboardResponse response =
                dashboardService.getResidentDashboard(user.getUserId());

        return ResponseEntity.ok(
                success("Resident dashboard fetched successfully", response)
        );
    }

    // ==========================================================================
    // 3. Security Dashboard - GET /api/dashboard/security
    // ==========================================================================

    @GetMapping("/security")
    @Operation(
            summary = "Get Security Dashboard",
            description = "Returns visitor monitoring statistics for security users."
    )
    public ResponseEntity<ApiResponse<SecurityDashboardResponse>> getSecurityDashboard() {

        SecurityDashboardResponse response =
                dashboardService.getSecurityDashboard();

        return ResponseEntity.ok(
                success("Security dashboard fetched successfully", response)
        );
    }

    // ==========================================================================
    // 4. Accountant Dashboard - GET /api/dashboard/accountant
    // ==========================================================================

    @GetMapping("/accountant")
    @Operation(
            summary = "Get Accountant Dashboard",
            description = "Returns billing and payment statistics for accountant users."
    )
    public ResponseEntity<ApiResponse<AccountantDashboardResponse>> getAccountantDashboard() {

        AccountantDashboardResponse response =
                dashboardService.getAccountantDashboard();

        return ResponseEntity.ok(
                success("Accountant dashboard fetched successfully", response)
        );
    }

    // ==========================================================================
    // 5. Success Response Builder
    // ==========================================================================

    private <T> ApiResponse<T> success(String message, T data) {

        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
}