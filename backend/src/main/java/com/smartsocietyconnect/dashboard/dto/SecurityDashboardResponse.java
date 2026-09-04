package com.smartsocietyconnect.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for Security dashboard summary.
 *
 * <p>This dashboard focuses on visitor monitoring, approval status,
 * and check-in/check-out activity for the current day.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityDashboardResponse {

    // ==========================================================================
    // 1. Visitor Summary
    // ==========================================================================

    private Long todayVisitors;

    private Long pendingVisitors;

    private Long approvedVisitors;

    private Long rejectedVisitors;

    private Long checkedInVisitors;

    private Long checkedOutVisitors;
}