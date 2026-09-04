package com.smartsocietyconnect.dashboard.service;

import com.smartsocietyconnect.dashboard.dto.AccountantDashboardResponse;
import com.smartsocietyconnect.dashboard.dto.AdminDashboardResponse;
import com.smartsocietyconnect.dashboard.dto.ResidentDashboardResponse;
import com.smartsocietyconnect.dashboard.dto.SecurityDashboardResponse;

/**
 * Service interface for Dashboard Module.
 *
 * <p>Provides dashboard statistics for different user roles:
 * <ul>
 *     <li>{@code ADMIN}</li>
 *     <li>{@code RESIDENT}</li>
 *     <li>{@code SECURITY}</li>
 *     <li>{@code ACCOUNTANT}</li>
 * </ul>
 *
 * <p>The dashboard module is read-only. It does not create, update, or delete
 * records. It only aggregates data from existing modules:
 * <ul>
 *     <li>Resident Module</li>
 *     <li>Flat Module</li>
 *     <li>Visitor Module</li>
 *     <li>Complaint Module</li>
 *     <li>Billing Module</li>
 *     <li>Payment Module</li>
 * </ul>
 *
 * <p>Implementations should use efficient repository count and sum queries
 * instead of loading full entity lists only to calculate statistics in memory.
 */
public interface DashboardService {

    // ==========================================================================
    // 1. Admin Dashboard
    // ==========================================================================

    /**
     * Return society-wide dashboard statistics for admin users.
     *
     * <p>Includes resident, flat, visitor, complaint, billing, and revenue
     * summary data.
     *
     * @return admin dashboard response
     */
    AdminDashboardResponse getAdminDashboard();

    // ==========================================================================
    // 2. Resident Dashboard
    // ==========================================================================

    /**
     * Return dashboard statistics for the logged-in resident.
     *
     * <p>The input is {@code userId}, not {@code residentId}, because the
     * authenticated JWT/session identifies the user account. The service layer
     * should resolve the resident profile using:
     *
     * <pre>
     * residentRepository.findByUserUserId(userId)
     * </pre>
     *
     * <p>This avoids exposing resident IDs in the API request and keeps the
     * dashboard tied to the authenticated user.
     *
     * @param userId authenticated user's ID
     * @return resident dashboard response
     */
    ResidentDashboardResponse getResidentDashboard(Integer userId);

    // ==========================================================================
    // 3. Security Dashboard
    // ==========================================================================

    /**
     * Return visitor monitoring statistics for security users.
     *
     * <p>Includes visitor counts by today's date and visitor workflow status.
     *
     * @return security dashboard response
     */
    SecurityDashboardResponse getSecurityDashboard();

    // ==========================================================================
    // 4. Accountant Dashboard
    // ==========================================================================

    /**
     * Return billing and payment statistics for accountant users.
     *
     * <p>Includes bill counts, today's collection, monthly collection, and
     * outstanding amount.
     *
     * @return accountant dashboard response
     */
    AccountantDashboardResponse getAccountantDashboard();
}