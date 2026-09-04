package com.smartsocietyconnect.dashboard.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for Accountant dashboard summary.
 *
 * <p>This dashboard focuses on billing and payment collection statistics.
 *
 * <p>All monetary values use {@link BigDecimal} to avoid precision problems.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountantDashboardResponse {

    // ==========================================================================
    // 1. Billing Summary
    // ==========================================================================

    private Long totalBills;

    private Long paidBills;

    private Long pendingBills;

    // ==========================================================================
    // 2. Collection Summary
    // ==========================================================================

    private BigDecimal todayCollection;

    private BigDecimal monthlyCollection;

    private BigDecimal outstandingAmount;
}