package com.smartsocietyconnect.dashboard.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for Resident dashboard summary.
 *
 * <p>This dashboard shows information related to one resident only, such as
 * visitor count, complaints, bills, and outstanding amount.
 *
 * <p>In the final secured version, the resident should normally be resolved
 * from the logged-in JWT user instead of being freely passed by the client.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentDashboardResponse {

    // ==========================================================================
    // 1. Resident Identity
    // ==========================================================================

    private Integer residentId;

    private String residentName;

    private String flatNumber;

    // ==========================================================================
    // 2. Visitor Summary
    // ==========================================================================

    private Long todayVisitors;

    private Long expectedVisitors;

    // ==========================================================================
    // 3. Complaint Summary
    // ==========================================================================

    private Long myComplaints;

    private Long openComplaints;

    // ==========================================================================
    // 4. Billing Summary
    // ==========================================================================

    private Long pendingBills;

    private Long paidBills;

    private BigDecimal outstandingAmount;
}