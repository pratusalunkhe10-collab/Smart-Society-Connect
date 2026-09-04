package com.smartsocietyconnect.dashboard.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for Admin dashboard summary.
 *
 * <p>The admin dashboard shows society-wide statistics across residents,
 * flats, visitors, complaints, billing, and revenue.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {

    // ==========================================================================
    // 1. Resident Statistics
    // ==========================================================================

    private Long totalResidents;

    private Long activeResidents;

    private Long inactiveResidents;

    // ==========================================================================
    // 2. Flat Statistics
    // ==========================================================================

    private Long totalFlats;

    private Long occupiedFlats;

    private Long vacantFlats;

    // ==========================================================================
    // 3. Visitor Statistics
    // ==========================================================================

    private Long todayVisitors;

    private Long pendingVisitors;

    private Long approvedVisitors;

    private Long rejectedVisitors;

    // ==========================================================================
    // 4. Complaint Statistics
    // ==========================================================================

    private Long totalComplaints;

    private Long openComplaints;

    private Long inProgressComplaints;

    private Long resolvedComplaints;

    // ==========================================================================
    // 5. Billing Statistics
    // ==========================================================================

    private Long totalBills;

    private Long paidBills;

    private Long pendingBills;

    // ==========================================================================
    // 6. Revenue Statistics
    // ==========================================================================

    private BigDecimal totalCollection;

    private BigDecimal todayCollection;

    private BigDecimal monthlyCollection;

    private BigDecimal pendingAmount;
}