package com.smartsocietyconnect.billing.service;

import java.util.List;

import com.smartsocietyconnect.billing.dto.BillingRequest;
import com.smartsocietyconnect.billing.dto.BillingResponse;
import com.smartsocietyconnect.billing.enums.BillingStatus;

/**
 * Service contract for Billing module operations.
 *
 * <p>This interface defines all business operations related to society bills.
 *
 * <p>Implementation responsibilities include:
 * <ul>
 *     <li>Validating residents before bill generation.</li>
 *     <li>Preventing duplicate bills for the same resident/month/year.</li>
 *     <li>Calculating total bill amount.</li>
 *     <li>Updating bill status based on payment or overdue rules.</li>
 *     <li>Mapping Billing entities to response DTOs.</li>
 * </ul>
 */
public interface BillingService {

    // ==========================================================================
    // 1. Create / Update / Delete
    // ==========================================================================

    /**
     * Generate a new monthly bill for a resident.
     *
     * @param request billing creation request
     * @return generated bill response
     */
    BillingResponse createBill(BillingRequest request);

    /**
     * Update an existing bill.
     *
     * <p>Usually only {@code PENDING}, {@code PARTIALLY_PAID}, or
     * {@code OVERDUE} bills should be editable depending on your business rule.
     *
     * @param billingId billing UUID
     * @param request updated billing data
     * @return updated bill response
     */
    BillingResponse updateBill(
            String billingId,
            BillingRequest request
    );

    /**
     * Delete a billing record.
     *
     * <p>Recommended rule: only unpaid/cancelled bills should be deletable.
     *
     * @param billingId billing UUID
     */
    void deleteBill(String billingId);

    // ==========================================================================
    // 2. Read Operations
    // ==========================================================================

    /**
     * Get bill by billing ID.
     *
     * @param billingId billing UUID
     * @return bill response
     */
    BillingResponse getBillById(String billingId);

    /**
     * Get all billing records.
     *
     * @return all bills
     */
    List<BillingResponse> getAllBills();

    /**
     * Get all bills for one resident.
     *
     * @param residentId resident primary key
     * @return resident billing records
     */
    List<BillingResponse> getBillsByResident(Integer residentId);

    /**
     * Get bills by billing status.
     *
     * @param status billing status
     * @return bills matching the given status
     */
    List<BillingResponse> getBillsByStatus(BillingStatus status);

    // ==========================================================================
    // 3. Status Operations
    // ==========================================================================

    /**
     * Mark a bill as overdue.
     *
     * <p>This can be called manually by admin or from a scheduled job.
     *
     * @param billingId billing UUID
     * @return updated bill response
     */
    BillingResponse markAsOverdue(String billingId);
}